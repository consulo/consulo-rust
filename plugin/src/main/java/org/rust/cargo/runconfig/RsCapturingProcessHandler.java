/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.cargo.runconfig;

import consulo.application.progress.ProgressIndicator;
import consulo.process.ExecutionException;
import consulo.process.KillableProcessHandler;
import consulo.process.ProcessHandler;
import consulo.process.cmd.GeneralCommandLine;
import consulo.process.event.ProcessListener;
import consulo.process.local.ProcessHandlerFactory;
import consulo.process.util.CapturingProcessRunner;
import consulo.process.util.ProcessOutput;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.rust.stdext.RsResult;

/**
 * Runs a command line and captures its stdout/stderr.
 * <p>
 * Built on public API only: {@link ProcessHandlerFactory} creates the handler and
 * {@link CapturingProcessRunner} does the capturing. Consulo's own
 * {@code consulo.process.internal.CapturingProcessHandler} — which pairs both roles in one class —
 * lives in a package exported only to platform modules, so a plugin cannot use it.
 * <p>
 * A <i>killable</i> handler is requested because callers need forcible termination when the owning
 * {@code Disposable} is disposed.
 */
public final class RsCapturingProcessHandler {

    @Nonnull
    private final ProcessHandler processHandler;
    @Nonnull
    private final CapturingProcessRunner runner;

    private RsCapturingProcessHandler(@Nonnull GeneralCommandLine commandLine) throws ExecutionException {
        this.processHandler = ProcessHandlerFactory.getInstance().createKillableProcessHandler(commandLine);
        this.runner = new CapturingProcessRunner(processHandler);
    }

    @Nonnull
    public ProcessHandler getProcessHandler() {
        return processHandler;
    }

    public ProcessOutput runProcess() {
        return runner.runProcess();
    }

    public ProcessOutput runProcess(int timeoutMs) {
        return runner.runProcess(timeoutMs);
    }

    public ProcessOutput runProcessWithProgressIndicator(@Nonnull ProgressIndicator indicator) {
        return runner.runProcess(indicator);
    }

    public ProcessOutput runProcessWithProgressIndicator(@Nonnull ProgressIndicator indicator, int timeoutMs) {
        return runner.runProcess(indicator, timeoutMs);
    }

    public boolean isProcessTerminated() {
        return processHandler.isProcessTerminated();
    }

    /**
     * Terminates the process as forcibly as the platform allows. The public {@link ProcessHandler}
     * contract exposes no {@link Process} handle, so {@code Process.destroyForcibly()} is
     * approximated by {@link KillableProcessHandler#killProcess()} where supported.
     */
    public void destroyProcess() {
        if (processHandler instanceof KillableProcessHandler killable && killable.canKillProcess()) {
            killable.killProcess();
        }
        else {
            processHandler.destroyProcess();
        }
    }

    public void addProcessListener(@Nonnull ProcessListener listener) {
        processHandler.addProcessListener(listener);
    }

    public void startNotify() {
        processHandler.startNotify();
    }

    @Nullable
    public java.io.OutputStream getProcessInput() {
        return processHandler.getProcessInput();
    }

    @Nonnull
    public static RsResult<RsCapturingProcessHandler, ExecutionException> startProcess(@Nonnull GeneralCommandLine commandLine) {
        try {
            return new RsResult.Ok<>(new RsCapturingProcessHandler(commandLine));
        } catch (ExecutionException e) {
            return new RsResult.Err<>(e);
        }
    }
}
