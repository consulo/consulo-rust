/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.cargo.runconfig.buildtool;

import com.intellij.execution.process.ProcessHandler;
import com.intellij.execution.runners.ExecutionEnvironment;
import com.intellij.openapi.progress.ProcessCanceledException;
import com.intellij.openapi.ui.MessageType;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.util.NlsContexts;
import com.intellij.openapi.util.UserDataHolderEx;
import org.rust.RsBundle;
import org.rust.cargo.project.model.CargoProject;
import org.rust.cargo.runconfig.RsExecutableRunner;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

public class CargoBuildContext extends CargoBuildContextBase {
    private static final Key<Semaphore> BUILD_SEMAPHORE_KEY = Key.create("BUILD_SEMAPHORE_KEY");

    private final ExecutionEnvironment environment;
    @NlsContexts.ProgressTitle
    private final String taskName;
    private volatile ProcessHandler processHandler;
    private final Semaphore buildSemaphore;
    private final CompletableFuture<CargoBuildResult> result = new CompletableFuture<>();
    private final long started = System.currentTimeMillis();
    private volatile long finished = started;

    public CargoBuildContext(
        CargoProject cargoProject,
        ExecutionEnvironment environment,
        @NlsContexts.ProgressTitle String taskName,
        @NlsContexts.ProgressText String progressTitle,
        boolean isTestBuild,
        Object buildId,
        Object parentId
    ) {
        super(cargoProject, progressTitle, isTestBuild, buildId, parentId);
        this.environment = environment;
        this.taskName = taskName;

        Semaphore existing = getProject().getUserData(BUILD_SEMAPHORE_KEY);
        if (existing != null) {
            this.buildSemaphore = existing;
        } else {
            this.buildSemaphore = ((UserDataHolderEx) getProject()).putUserDataIfAbsent(BUILD_SEMAPHORE_KEY, new Semaphore(1));
        }
    }

    public ExecutionEnvironment getEnvironment() {
        return environment;
    }

    public String getTaskName() {
        return taskName;
    }

    public ProcessHandler getProcessHandler() {
        return processHandler;
    }

    public void setProcessHandler(ProcessHandler processHandler) {
        this.processHandler = processHandler;
    }

    public CompletableFuture<CargoBuildResult> getResult() {
        return result;
    }

    public long getStarted() {
        return started;
    }

    public long getFinished() {
        return finished;
    }

    private long getDuration() {
        return finished - started;
    }

    public boolean waitAndStart() {
        if (getIndicator() != null) {
            getIndicator().pushState();
        }
        try {
            if (getIndicator() != null) {
                getIndicator().setText(RsBundle.message("progress.text.waiting.for.current.build.to.finish"));
                getIndicator().setText2("");
            }
            while (true) {
                if (getIndicator() != null) {
                    getIndicator().checkCanceled();
                }
                try {
                    if (buildSemaphore.tryAcquire(100, TimeUnit.MILLISECONDS)) break;
                } catch (InterruptedException e) {
                    throw new ProcessCanceledException();
                }
            }
        } catch (ProcessCanceledException e) {
            canceled();
            return false;
        } finally {
            if (getIndicator() != null) {
                getIndicator().popState();
            }
        }
        return true;
    }

    public void finished(boolean isSuccess) {
        boolean isCanceled = getIndicator() != null && getIndicator().isCanceled();

        RsExecutableRunner.setArtifacts(environment, isSuccess && !isCanceled ? getArtifacts() : null);

        finished = System.currentTimeMillis();
        buildSemaphore.release();

        String finishMessage;
        String finishDetails;

        int errors = getErrors().get();
        int warnings = getWarnings().get();

        MessageType messageType;
        if (isCanceled) {
            finishMessage = RsBundle.message("system.notification.title.canceled", taskName);
            finishDetails = null;
            messageType = MessageType.INFO;
        } else {
            boolean hasWarningsOrErrors = errors > 0 || warnings > 0;
            finishMessage = isSuccess
                ? RsBundle.message("system.notification.title.finished", taskName)
                : RsBundle.message("system.notification.title.failed", taskName);
            if (hasWarningsOrErrors) {
                String errorsString = errors == 1 ? "error" : "errors";
                String warningsString = warnings == 1 ? "warning" : "warnings";
                finishDetails = RsBundle.message("system.notification.text.", errors, errorsString, warnings, warningsString);
            } else {
                finishDetails = null;
            }

            if (!isSuccess) {
                messageType = MessageType.ERROR;
            } else if (hasWarningsOrErrors) {
                messageType = MessageType.WARNING;
            } else {
                messageType = MessageType.INFO;
            }
        }

        result.complete(new CargoBuildResult(
            isSuccess,
            isCanceled,
            started,
            getDuration(),
            errors,
            warnings,
            finishMessage
        ));

        CargoBuildManager.INSTANCE.showBuildNotification(getProject(), messageType, finishMessage, finishDetails, getDuration());
    }

    public void canceled() {
        finished = System.currentTimeMillis();

        result.complete(new CargoBuildResult(
            false,
            true,
            started,
            getDuration(),
            getErrors().get(),
            getWarnings().get(),
            taskName + " canceled"
        ));

        Utils.notifyProcessNotStarted(environment);
    }
}
