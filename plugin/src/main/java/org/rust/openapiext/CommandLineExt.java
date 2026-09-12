/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.openapiext;

import consulo.process.cmd.GeneralCommandLine;
import consulo.platform.Platform;
import consulo.process.event.ProcessListener;
import consulo.process.util.ProcessOutput;
import consulo.disposer.Disposable;
import consulo.application.ReadAction;
import consulo.logging.Logger;
import consulo.application.progress.ProgressIndicator;
import consulo.application.progress.ProgressManager;
import consulo.disposer.Disposer;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.rust.RsBundle;
import org.rust.cargo.runconfig.RsCapturingProcessHandler;
import org.rust.stdext.RsResult;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Path;
import consulo.process.ExecutionException;

public final class CommandLineExt {
    private static final Logger LOG = Logger.getInstance("org.rust.openapiext.CommandLineExt");

    private CommandLineExt() {
    }

    @Nonnull
    public static GeneralCommandLine newCommandLine(@Nonnull Path path, boolean withSudo, @Nonnull String... args) {
        String[] allArgs = new String[args.length + 1];
        allArgs[0] = path.toString().replace('\\', '/');
        System.arraycopy(args, 0, allArgs, 1, args.length);
        GeneralCommandLine commandLine = new GeneralCommandLine(allArgs);
        if (withSudo) {
            commandLine.withSudo(Platform.current().os().isWindows()
                ? RsBundle.message("checkbox.run.with.administrator.privileges")
                : RsBundle.message("checkbox.run.with.root.privileges"));
        }
        return commandLine;
    }

    @Nonnull
    public static GeneralCommandLine withWorkDirectory(@Nonnull GeneralCommandLine commandLine, @Nullable Path path) {
        return commandLine.withWorkDirectory(path != null ? path.toString().replace('\\', '/') : null);
    }

    @Nullable
    public static ProcessOutput execute(@Nonnull GeneralCommandLine commandLine, @Nullable Integer timeoutInMilliseconds) {
        LOG.info("Executing `" + commandLine.getCommandLineString() + "`");
        RsResult<RsCapturingProcessHandler, ? extends Exception> result = RsCapturingProcessHandler.startProcess(commandLine);
        if (result.isErr()) {
            LOG.warn("Failed to run executable", (Throwable) result.err());
            return null;
        }
        RsCapturingProcessHandler handler = result.unwrap();
        ProcessOutput output = runProcessWithGlobalProgress(handler, timeoutInMilliseconds);

        if (!isSuccess(output)) {
            LOG.warn(RsProcessExecutionException.errorMessage(commandLine.getCommandLineString(), output));
        }

        return output;
    }

    @Nonnull
    public static RsResult<ProcessOutput, RsProcessExecutionException> execute(
        @Nonnull GeneralCommandLine commandLine,
        @Nonnull Disposable owner,
        @Nullable byte[] stdIn,
        @Nullable ProcessListener listener
    ) {
        return execute(commandLine, owner, stdIn, listener, null);
    }

    @Nonnull
    public static RsResult<ProcessOutput, RsProcessExecutionException> execute(
        @Nonnull GeneralCommandLine commandLine,
        @Nonnull Disposable owner,
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
                new consulo.process.ExecutionException(ex.getMessage(), ex)));
        }

        RsCapturingProcessHandler handler = startResult.unwrap();

        Disposable cargoKiller = () -> {
            if (!handler.isProcessTerminated()) {
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

    @Nonnull
    private static ProcessOutput runProcessWithGlobalProgress(@Nonnull RsCapturingProcessHandler handler,
                                                              @Nullable Integer timeoutInMilliseconds) {
        return runProcess(handler, ProgressManager.getGlobalProgressIndicator(), timeoutInMilliseconds);
    }

    @Nonnull
    public static ProcessOutput runProcess(@Nonnull RsCapturingProcessHandler handler,
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

    public static boolean isSuccess(@Nonnull ProcessOutput output) {
        return !output.isTimeout() && !output.isCancelled() && output.getExitCode() == 0;
    }
}
