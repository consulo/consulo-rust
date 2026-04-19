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
import com.google.common.annotations.VisibleForTesting;
import com.intellij.execution.configuration.EnvironmentVariablesData;
import com.intellij.execution.process.ProcessIOExecutorService;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.util.Disposer;
import com.intellij.util.concurrency.AppExecutorUtil;
import org.rust.stdext.PathUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
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

    @NotNull
    private final List<ProcMacroServerProcess> myStack;
    @NotNull
    private final Lock myStackLock;
    @NotNull
    private final Condition myStackIsNotEmpty;
    @NotNull
    private final ScheduledFuture<?> myIdleProcessCleaner;
    @NotNull
    private final RsToolchainBase myToolchain;
    @NotNull
    private final Path myExpanderExecutable;
    private final boolean myNeedsVersionCheck;
    private volatile boolean myIsDisposed;

    // Lazy version check result
    private volatile RsResult<ProMacroExpanderVersion, RequestSendError> myExpanderVersion;

    private ProcMacroServerPool(
        @NotNull RsToolchainBase toolchain,
        boolean needsVersionCheck,
        @NotNull Path expanderExecutable
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

    @NotNull
    public static ProcMacroServerPool create(
        @NotNull RsToolchainBase toolchain,
        boolean needsVersionCheck,
        @NotNull Path expanderExecutable,
        @NotNull Disposable parentDisposable
    ) {
        ProcMacroServerPool pool = new ProcMacroServerPool(toolchain, needsVersionCheck, expanderExecutable);
        Disposer.register(parentDisposable, pool);
        return pool;
    }

    @NotNull
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

    @NotNull
    public RsResult<Response, RequestSendError> send(@NotNull Request request, long timeout) {
        RsResult<ProMacroExpanderVersion, RequestSendError> versionResult = requestExpanderVersion();
        if (versionResult instanceof RsResult.Err) {
            @SuppressWarnings("unchecked")
            RsResult.Err<Response, RequestSendError> err = new RsResult.Err<>(((RsResult.Err<ProMacroExpanderVersion, RequestSendError>) versionResult).getErr());
            return err;
        }
        return sendInner(request, timeout);
    }

    @NotNull
    private RsResult<Response, RequestSendError> sendInner(@NotNull Request request, long timeout) {
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

    @NotNull
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

    @NotNull
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
    public static Path findExpanderExecutablePath(@NotNull RsToolchainBase toolchain, @NotNull String sysroot) {
        Path fromToolchain = findExpanderFromToolchain(toolchain, sysroot);
        if (fromToolchain != null) return fromToolchain;
        return findEmbeddedExpander(toolchain);
    }

    @Nullable
    private static Path findExpanderFromToolchain(@NotNull RsToolchainBase toolchain, @NotNull String sysroot) {
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
    private static Path findEmbeddedExpander(@NotNull RsToolchainBase toolchain) {
        return RsPathManager.INSTANCE.nativeHelper(toolchain instanceof RsWslToolchain);
    }
}

/**
 * {@link ProcMacroServerProcess} is responsible for communicating with the proc macro expander process
 * and manages its lifecycle.
 */
class ProcMacroServerProcess implements Runnable, Disposable {

    @NotNull
    private final Process myProcess;
    private final boolean myIsWsl;
    @NotNull
    private final BufferedReader myStdout;
    @NotNull
    private final Writer myStdin;
    @NotNull
    private final ReentrantLock myLock;
    @NotNull
    private final SynchronousQueue<RequestEntry> myRequestQueue;
    @NotNull
    private final Future<?> myTask;

    private volatile long myLastUsed;
    private volatile boolean myIsDisposed;
    private boolean myIsFirstRequest;

    private ProcMacroServerProcess(@NotNull Process process, boolean isWsl) {
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

    @NotNull
    Response send(@NotNull Request request, long timeout) throws IOException, TimeoutException {
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
    private Throwable tryRefineException(@NotNull Throwable e) {
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

    @NotNull
    private Response writeAndRead(@NotNull Request request) throws IOException {
        ProcMacroJsonParser.JSON_MAPPER.writeValue(myStdin, request);
        myStdin.write("\n");
        myStdin.flush();

        skipUntilJsonObject(myStdout);

        return ProcMacroJsonParser.JSON_MAPPER.readValue(myStdout, Response.class);
    }

    private static void skipUntilJsonObject(@NotNull BufferedReader reader) throws IOException {
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

    @NotNull
    static ProcMacroServerProcess createAndRun(@NotNull RsToolchainBase toolchain, @NotNull Path expanderExecutable)
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
            process = commandLine.toProcessBuilder()
                .redirectError(ProcessBuilder.Redirect.DISCARD)
                .start();
        } catch (IOException e) {
            throw new ProcessCreationException(e);
        } catch (com.intellij.execution.ExecutionException e) {
            throw new ProcessCreationException(new IOException(e));
        }

        MacroExpansionManagerUtil.MACRO_LOG.debug("Started proc macro expander process (pid: " + process.pid() + ")");

        return new ProcMacroServerProcess(process, toolchain instanceof RsWslToolchain);
    }

    private static final class RequestEntry {
        @NotNull
        final Request request;
        @NotNull
        final CompletableFuture<Response> future;

        RequestEntry(@NotNull Request request, @NotNull CompletableFuture<Response> future) {
            this.request = request;
            this.future = future;
        }
    }
}

/**
 * JSON parser configuration for proc macro communication.
 */
@VisibleForTesting
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
