/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.openapiext;

import com.intellij.execution.configurations.GeneralCommandLine;
import com.intellij.execution.process.CapturingProcessHandler;
import com.intellij.execution.process.ElevationService;
import com.intellij.execution.process.ProcessListener;
import com.intellij.execution.process.ProcessOutput;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.application.ReadAction;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.util.Disposer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.rust.cargo.runconfig.RsCapturingProcessHandler;
import org.rust.stdext.RsResult;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Path;

public final class CommandLineExt {
    private static final Logger LOG = Logger.getInstance("org.rust.openapiext.CommandLineExt");

    private CommandLineExt() {
    }

    @NotNull
    public static GeneralCommandLine newCommandLine(@NotNull Path path, boolean withSudo, @NotNull String... args) {
        String[] allArgs = new String[args.length + 1];
        allArgs[0] = path.toString().replace('\\', '/');
        System.arraycopy(args, 0, allArgs, 1, args.length);
        return new GeneralCommandLine(allArgs) {
            @NotNull
            @Override
            public Process createProcess() throws com.intellij.execution.ExecutionException {
                if (withSudo) {
                    return ElevationService.getInstance().createProcess(this);
                } else {
                    return super.createProcess();
                }
            }
        };
    }

    @NotNull
    public static GeneralCommandLine withWorkDirectory(@NotNull GeneralCommandLine commandLine, @Nullable Path path) {
        return commandLine.withWorkDirectory(path != null ? path.toString().replace('\\', '/') : null);
    }

    @Nullable
    public static ProcessOutput execute(@NotNull GeneralCommandLine commandLine, @Nullable Integer timeoutInMilliseconds) {
        LOG.info("Executing `" + commandLine.getCommandLineString() + "`");
        RsResult<RsCapturingProcessHandler, ? extends Exception> result = RsCapturingProcessHandler.startProcess(commandLine);
        if (result.isErr()) {
            LOG.warn("Failed to run executable", (Throwable) result.err());
            return null;
        }
        CapturingProcessHandler handler = (CapturingProcessHandler) result.unwrap();
        ProcessOutput output = runProcessWithGlobalProgress(handler, timeoutInMilliseconds);

        if (!isSuccess(output)) {
            LOG.warn(RsProcessExecutionException.errorMessage(commandLine.getCommandLineString(), output));
        }

        return output;
    }

    @NotNull
    public static RsResult<ProcessOutput, RsProcessExecutionException> execute(
        @NotNull GeneralCommandLine commandLine,
        @NotNull Disposable owner,
        @Nullable byte[] stdIn,
        @Nullable ProcessListener listener
    ) {
        return execute(commandLine, owner, stdIn, listener, null);
    }

    @NotNull
    public static RsResult<ProcessOutput, RsProcessExecutionException> execute(
        @NotNull GeneralCommandLine commandLine,
        @NotNull Disposable owner,
        @Nullable byte[] stdIn,
        @Nullable ProcessListener listener,
        @Nullable Integer timeoutInMilliseconds
    ) {
        LOG.info("Executing `" + commandLine.getCommandLineString() + "`");

        RsResult<RsCapturingProcessHandler, ? extends Exception> startResult = RsCapturingProcessHandler.startProcess(commandLine);
        if (startResult.isErr()) {
            Exception ex = (Exception) startResult.err();
            LOG.warn("Failed to run executable", ex);
            return new RsResult.Err<>(new RsProcessExecutionException.Start(
                commandLine.getCommandLineString(),
                new com.intellij.execution.ExecutionException(ex.getMessage(), ex)));
        }

        CapturingProcessHandler handler = (CapturingProcessHandler) startResult.unwrap();

        Disposable cargoKiller = () -> {
            if (!handler.isProcessTerminated()) {
                handler.getProcess().destroyForcibly();
                handler.destroyProcess();
            }
        };

        boolean alreadyDisposed = ReadAction.compute(() -> {
            if (Disposer.isDisposed(owner)) {
                return true;
            } else {
                Disposer.register(owner, cargoKiller);
                return false;
            }
        });

        if (alreadyDisposed) {
            Disposer.dispose(cargoKiller);
            ProcessOutput output = new ProcessOutput();
            output.setCancelled();
            return new RsResult.Err<>(new RsProcessExecutionException.Canceled(
                commandLine.getCommandLineString(), output, "Command failed to start"));
        }

        if (listener != null) {
            handler.addProcessListener(listener);
        }

        ProcessOutput output;
        try {
            if (stdIn != null) {
                try (OutputStream inputStream = handler.getProcessInput()) {
                    inputStream.write(stdIn);
                } catch (IOException e) {
                    // ignore
                }
            }
            output = runProcessWithGlobalProgress(handler, timeoutInMilliseconds);
        } finally {
            Disposer.dispose(cargoKiller);
        }

        if (output.isCancelled()) {
            return new RsResult.Err<>(new RsProcessExecutionException.Canceled(
                commandLine.getCommandLineString(), output));
        } else if (output.isTimeout()) {
            return new RsResult.Err<>(new RsProcessExecutionException.Timeout(
                commandLine.getCommandLineString(), output));
        } else if (output.getExitCode() != 0) {
            return new RsResult.Err<>(new RsProcessExecutionException.ProcessAborted(
                commandLine.getCommandLineString(), output));
        } else {
            return new RsResult.Ok<>(output);
        }
    }

    @NotNull
    private static ProcessOutput runProcessWithGlobalProgress(@NotNull CapturingProcessHandler handler,
                                                              @Nullable Integer timeoutInMilliseconds) {
        return runProcess(handler, ProgressManager.getGlobalProgressIndicator(), timeoutInMilliseconds);
    }

    @NotNull
    public static ProcessOutput runProcess(@NotNull CapturingProcessHandler handler,
                                           @Nullable ProgressIndicator indicator,
                                           @Nullable Integer timeoutInMilliseconds) {
        if (indicator != null && timeoutInMilliseconds != null) {
            return handler.runProcessWithProgressIndicator(indicator, timeoutInMilliseconds);
        } else if (indicator != null) {
            return handler.runProcessWithProgressIndicator(indicator);
        } else if (timeoutInMilliseconds != null) {
            return handler.runProcess(timeoutInMilliseconds);
        } else {
            return handler.runProcess();
        }
    }

    public static boolean isSuccess(@NotNull ProcessOutput output) {
        return !output.isTimeout() && !output.isCancelled() && output.getExitCode() == 0;
    }
}
