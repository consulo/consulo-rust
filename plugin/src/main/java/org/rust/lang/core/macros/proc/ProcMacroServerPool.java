/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.macros.proc;

import org.rust.stdext.Lazy;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.module.kotlin.KotlinModule;
import consulo.execution.configuration.EnvironmentVariablesData;
import consulo.process.io.ProcessIOExecutorService;
import consulo.disposer.Disposable;
import consulo.disposer.Disposer;
import consulo.application.util.concurrent.AppExecutorUtil;
import org.rust.stdext.PathUtil;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.rust.cargo.toolchain.BacktraceMode;
import org.rust.cargo.toolchain.RsToolchainBase;
import org.rust.cargo.toolchain.wsl.RsWslToolchain;
import org.rust.lang.core.macros.MacroExpansionManagerUtil;
import org.rust.lang.core.macros.tt.FlatTree;
import org.rust.lang.core.macros.tt.FlatTreeJsonDeserializer;
import org.rust.lang.core.macros.tt.FlatTreeJsonSerializer;
import org.rust.openapiext.RsPathManager;
import org.rust.openapiext.OpenApiUtil;
import org.rust.stdext.RsResult;

import java.io.*;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * A pool of proc macro expander server processes.
 */
public class ProcMacroServerPool implements Disposable {

    private static final int POOL_SIZE = 4;
    private static final long MAX_IDLE_MILLIS = 60_000L;

    @Nonnull
    private final List<ProcMacroServerProcess> myStack;
    @Nonnull
    private final Lock myStackLock;
    @Nonnull
    private final Condition myStackIsNotEmpty;
    @Nonnull
    private final ScheduledFuture<?> myIdleProcessCleaner;
    @Nonnull
    private final RsToolchainBase myToolchain;
    @Nonnull
    private final Path myExpanderExecutable;
    private final boolean myNeedsVersionCheck;
    private volatile boolean myIsDisposed;

    // Lazy version check result
    private volatile RsResult<ProMacroExpanderVersion, RequestSendError> myExpanderVersion;

    private ProcMacroServerPool(
        @Nonnull RsToolchainBase toolchain,
        boolean needsVersionCheck,
        @Nonnull Path expanderExecutable
    ) {
        myToolchain = toolchain;
        myNeedsVersionCheck = needsVersionCheck;
        myExpanderExecutable = expanderExecutable;
        myStack = new ArrayList<>();
        myStackLock = new ReentrantLock();
        myStackIsNotEmpty = myStackLock.newCondition();
        myIsDisposed = false;

        // Initialize pool with null slots
        for (int i = 0; i < POOL_SIZE; i++) {
            myStack.add(null);
        }

        myIdleProcessCleaner = AppExecutorUtil.getAppScheduledExecutorService()
            .scheduleWithFixedDelay(this::killIdleExpanders, MAX_IDLE_MILLIS, MAX_IDLE_MILLIS, TimeUnit.MILLISECONDS);
    }

    @Nonnull
    public static ProcMacroServerPool create(
        @Nonnull RsToolchainBase toolchain,
        boolean needsVersionCheck,
        @Nonnull Path expanderExecutable,
        @Nonnull Disposable parentDisposable
    ) {
        ProcMacroServerPool pool = new ProcMacroServerPool(toolchain, needsVersionCheck, expanderExecutable);
        Disposer.register(parentDisposable, pool);
        return pool;
    }

    @Nonnull
    public RsResult<ProMacroExpanderVersion, RequestSendError> requestExpanderVersion() {
        if (!myNeedsVersionCheck) {
            return new RsResult.Ok<>(ProMacroExpanderVersion.NO_VERSION_CHECK_VERSION);
        }
        if (myExpanderVersion == null) {
            synchronized (this) {
                if (myExpanderVersion == null) {
                    myExpanderVersion = sendInner(Request.API_VERSION_CHECK, 10_000L).andThen(response -> {
                        int i = (int) ((Response.ApiVersionCheck) response).getVersion();
                        ProMacroExpanderVersion version = ProMacroExpanderVersion.from(i);
                        if (version == null) {
                            return new RsResult.Err<>(new RequestSendError.UnknownVersion(i));
                        }
                        return new RsResult.Ok<>(version);
                    });
                }
            }
        }
        return myExpanderVersion;
    }

    @Nonnull
    public RsResult<Response, RequestSendError> send(@Nonnull Request request, long timeout) {
        RsResult<ProMacroExpanderVersion, RequestSendError> versionResult = requestExpanderVersion();
        if (versionResult instanceof RsResult.Err) {
            @SuppressWarnings("unchecked")
            RsResult.Err<Response, RequestSendError> err = new RsResult.Err<>(((RsResult.Err<ProMacroExpanderVersion, RequestSendError>) versionResult).getErr());
            return err;
        }
        return sendInner(request, timeout);
    }

    @Nonnull
    private RsResult<Response, RequestSendError> sendInner(@Nonnull Request request, long timeout) {
        ProcMacroServerProcess io;
        try {
            io = alloc();
        } catch (ProcessCreationException e) {
            return new RsResult.Err<>(new RequestSendError.ProcessCreation(e));
        }
        try {
            Response response = io.send(request, timeout);
            return new RsResult.Ok<>(response);
        } catch (IOException e) {
            return new RsResult.Err<>(new RequestSendError.IO(e));
        } catch (TimeoutException e) {
            return new RsResult.Err<>(new RequestSendError.Timeout(e));
        } finally {
            free(io);
        }
    }

    @Nonnull
    private ProcMacroServerProcess alloc() throws ProcessCreationException {
        if (myIsDisposed) throw new IllegalStateException("Pool is disposed");
        ProcMacroServerProcess value;
        myStackLock.lock();
        try {
            while (myStack.isEmpty()) {
                try {
                    myStackIsNotEmpty.await();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new ProcessCreationException(new IOException("Interrupted while waiting for process", e));
                }
            }
            value = myStack.remove(myStack.size() - 1);
        } finally {
            myStackLock.unlock();
        }

        if (value == null) {
            return supply();
        } else if (value.isValid()) {
            return value;
        } else {
            Disposer.dispose(value);
            return supply();
        }
    }

    @Nonnull
    private ProcMacroServerProcess supply() throws ProcessCreationException {
        ProcMacroServerProcess process;
        try {
            process = ProcMacroServerProcess.createAndRun(myToolchain, myExpanderExecutable);
        } catch (Throwable t) {
            free(null);
            if (t instanceof ProcessCreationException) throw (ProcessCreationException) t;
            throw new ProcessCreationException(new IOException(t));
        }
        Disposer.register(this, process);
        return process;
    }

    private void free(@Nullable ProcMacroServerProcess process) {
        myStackLock.lock();
        try {
            myStack.add(process);
            myStackIsNotEmpty.signal();
        } finally {
            myStackLock.unlock();
        }
    }

    private void killIdleExpanders() {
        List<ProcMacroServerProcess> toDispose = new ArrayList<>();
        myStackLock.lock();
        try {
            for (int i = 0; i < myStack.size(); i++) {
                ProcMacroServerProcess process = myStack.get(i);
                if (process != null && process.getIdleTime() > MAX_IDLE_MILLIS) {
                    myStack.set(i, null);
                    toDispose.add(process);
                }
            }
        } finally {
            myStackLock.unlock();
        }
        for (ProcMacroServerProcess process : toDispose) {
            Disposer.dispose(process);
        }
    }

    @Override
    public void dispose() {
        myIsDisposed = true;
        myIdleProcessCleaner.cancel(false);
        if (myStack.size() != POOL_SIZE) {
            MacroExpansionManagerUtil.MACRO_LOG.error("Some processes were not freed! " + myStack.size() + " != " + POOL_SIZE);
        }
    }

    @Nullable
    public static Path findExpanderExecutablePath(@Nonnull RsToolchainBase toolchain, @Nonnull String sysroot) {
        Path fromToolchain = findExpanderFromToolchain(toolchain, sysroot);
        if (fromToolchain != null) return fromToolchain;
        return findEmbeddedExpander(toolchain);
    }

    @Nullable
    private static Path findExpanderFromToolchain(@Nonnull RsToolchainBase toolchain, @Nonnull String sysroot) {
        String binaryName = toolchain.getExecutableName("rust-analyzer-proc-macro-srv");
        Path expanderPath = Path.of(sysroot, "libexec", binaryName);

        if (toolchain instanceof RsWslToolchain) {
            if (!expanderPath.toFile().isFile()) return null;
        } else {
            if (!expanderPath.toFile().canExecute()) return null;
        }
        return expanderPath;
    }

    @Nullable
    private static Path findEmbeddedExpander(@Nonnull RsToolchainBase toolchain) {
        return RsPathManager.INSTANCE.nativeHelper(toolchain instanceof RsWslToolchain);
    }
}

/**
 * {@link ProcMacroServerProcess} is responsible for communicating with the proc macro expander process
 * and manages its lifecycle.
 */
class ProcMacroServerProcess implements Runnable, Disposable {

    @Nonnull
    private final Process myProcess;
    private final boolean myIsWsl;
    @Nonnull
    private final BufferedReader myStdout;
    @Nonnull
    private final Writer myStdin;
    @Nonnull
    private final ReentrantLock myLock;
    @Nonnull
    private final SynchronousQueue<RequestEntry> myRequestQueue;
    @Nonnull
    private final Future<?> myTask;

    private volatile long myLastUsed;
    private volatile boolean myIsDisposed;
    private boolean myIsFirstRequest;

    private ProcMacroServerProcess(@Nonnull Process process, boolean isWsl) {
        myProcess = process;
        myIsWsl = isWsl;
        myStdout = new BufferedReader(new InputStreamReader(process.getInputStream(), java.nio.charset.StandardCharsets.UTF_8));
        myStdin = new OutputStreamWriter(process.getOutputStream(), java.nio.charset.StandardCharsets.UTF_8);
        myLock = new ReentrantLock();
        myRequestQueue = new SynchronousQueue<>();
        myTask = ProcessIOExecutorService.INSTANCE.submit(this);
        myLastUsed = System.currentTimeMillis();
        myIsDisposed = false;
        myIsFirstRequest = true;
    }

    @Nonnull
    Response send(@Nonnull Request request, long timeout) throws IOException, TimeoutException {
        if (!myLock.tryLock()) {
            throw new IllegalStateException("`send` must not be called from multiple threads simultaneously");
        }
        try {
            if (!myProcess.isAlive()) throw new IOException("The process has been killed");
            CompletableFuture<Response> responseFuture = new CompletableFuture<>();
            if (!myRequestQueue.offer(new RequestEntry(request, responseFuture), timeout, TimeUnit.MILLISECONDS)) {
                throw new TimeoutException();
            }
            try {
                return responseFuture.get(timeout, TimeUnit.MILLISECONDS);
            } catch (ExecutionException e) {
                Throwable cause = e.getCause();
                if (cause instanceof IOException) throw (IOException) cause;
                throw new IOException("Unexpected error", cause != null ? cause : e);
            } catch (InterruptedException e) {
                throw new IOException("Interrupted", e);
            }
        } catch (InterruptedException e) {
            throw new IOException("Interrupted while offering request", e);
        } catch (Throwable t) {
            Disposer.dispose(this);
            if (t instanceof IOException) throw (IOException) t;
            if (t instanceof TimeoutException) throw (TimeoutException) t;
            throw new IOException(t);
        } finally {
            myLastUsed = System.currentTimeMillis();
            myLock.unlock();
        }
    }

    boolean isValid() {
        return !myIsDisposed && myProcess.isAlive();
    }

    long getIdleTime() {
        return System.currentTimeMillis() - myLastUsed;
    }

    @Override
    public void run() {
        try {
            while (!myIsDisposed) {
                RequestEntry entry;
                try {
                    entry = myRequestQueue.take();
                } catch (InterruptedException e) {
                    return;
                }
                Response response;
                try {
                    response = writeAndRead(entry.request);
                } catch (Throwable e) {
                    Throwable refined = tryRefineException(e);
                    entry.future.completeExceptionally(refined != null ? refined : e);
                    return;
                }
                myIsFirstRequest = false;
                entry.future.complete(response);
            }
        } finally {
            if (!myIsDisposed) {
                killProcess();
            }
        }
    }

    @Nullable
    private Throwable tryRefineException(@Nonnull Throwable e) {
        if (myIsWsl && myIsFirstRequest && e instanceof IOException
            && "The pipe is being closed".equals(e.getMessage())) {
            return new ProcessCreationException((IOException) e);
        }

        boolean isEOF = e instanceof EOFException
            || e instanceof com.fasterxml.jackson.core.io.JsonEOFException
            || (e instanceof IOException && "Stream Closed".equals(e.getMessage()));
        long waitTimeout = OpenApiUtil.isUnitTestMode() ? 2000L : 100L;
        if (isEOF) {
            try {
                if (myProcess.waitFor(waitTimeout, TimeUnit.MILLISECONDS)) {
                    return new ProcessAbortedException(e, myProcess.exitValue());
                }
            } catch (InterruptedException ignored) {
            }
        }

        return null;
    }

    @Nonnull
    private Response writeAndRead(@Nonnull Request request) throws IOException {
        ProcMacroJsonParser.JSON_MAPPER.writeValue(myStdin, request);
        myStdin.write("\n");
        myStdin.flush();

        skipUntilJsonObject(myStdout);

        return ProcMacroJsonParser.JSON_MAPPER.readValue(myStdout, Response.class);
    }

    private static void skipUntilJsonObject(@Nonnull BufferedReader reader) throws IOException {
        while (true) {
            reader.mark(1);
            int ch = reader.read();
            if (ch == -1) throw new EOFException();
            if (ch == '{') {
                reader.reset();
                break;
            }
        }
    }

    @Override
    public void dispose() {
        myIsDisposed = true;
        myTask.cancel(true);
        killProcess();
    }

    private void killProcess() {
        MacroExpansionManagerUtil.MACRO_LOG.debug("Killing proc macro expander process (pid: " + myProcess.pid() + ")");
        myProcess.destroyForcibly();
    }

    private static final Path WORKING_DIR;

    static {
        Path dir;
        try {
            dir = RsPathManager.INSTANCE.tempPluginDirInSystem().resolve("proc-macro-expander-pwd");
            java.nio.file.Files.createDirectories(dir);
            // Clean directory
            org.rust.stdext.PathUtil.cleanDirectory(dir);
        } catch (IOException e) {
            MacroExpansionManagerUtil.MACRO_LOG.error(e);
            dir = Paths.get(".");
        }
        WORKING_DIR = dir;
    }

    @Nonnull
    static ProcMacroServerProcess createAndRun(@Nonnull RsToolchainBase toolchain, @Nonnull Path expanderExecutable)
        throws ProcessCreationException {
        MacroExpansionManagerUtil.MACRO_LOG.debug("Starting proc macro expander process " + expanderExecutable);

        Map<String, String> env = new HashMap<>();
        env.put("INTELLIJ_RUST", "1");
        env.put("RA_DONT_COPY_PROC_MACRO_DLL", "1");
        env.put("RUST_ANALYZER_INTERNALS_DO_NOT_USE", "this is unstable");

        var commandLine = toolchain.createGeneralCommandLine(
            expanderExecutable,
            WORKING_DIR,
            null,
            BacktraceMode.NO,
            EnvironmentVariablesData.create(env, true),
            Collections.emptyList(),
            false,
            false,
            true,
            com.intellij.util.net.HttpConfigurable.getInstance()
        ).withRedirectErrorStream(false);

        Process process;
        try {
            // toProcessBuilder isn't exposed in Consulo's GeneralCommandLine; use createProcess
            process = commandLine.createProcess();
        } catch (consulo.process.ExecutionException e) {
            throw new ProcessCreationException(new IOException(e));
        }

        MacroExpansionManagerUtil.MACRO_LOG.debug("Started proc macro expander process (pid: " + process.pid() + ")");

        return new ProcMacroServerProcess(process, toolchain instanceof RsWslToolchain);
    }

    private static final class RequestEntry {
        @Nonnull
        final Request request;
        @Nonnull
        final CompletableFuture<Response> future;

        RequestEntry(@Nonnull Request request, @Nonnull CompletableFuture<Response> future) {
            this.request = request;
            this.future = future;
        }
    }
}

/**
 * JSON parser configuration for proc macro communication.
 */

class ProcMacroJsonParser {
    static final ObjectMapper JSON_MAPPER = new ObjectMapper()
        .configure(JsonGenerator.Feature.AUTO_CLOSE_TARGET, false)
        .configure(JsonGenerator.Feature.FLUSH_PASSED_TO_STREAM, false)
        .configure(JsonGenerator.Feature.AUTO_CLOSE_JSON_CONTENT, false)
        .configure(JsonParser.Feature.AUTO_CLOSE_SOURCE, false)
        .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
        .registerModule(new KotlinModule.Builder().build())
        .registerModule(
            new SimpleModule()
                .addSerializer(Request.class, new RequestJsonSerializer())
                .addDeserializer(Response.class, new ResponseJsonDeserializer())
                .addDeserializer(FlatTree.class, FlatTreeJsonDeserializer.INSTANCE)
                .addSerializer(FlatTree.class, FlatTreeJsonSerializer.INSTANCE)
        );
}
