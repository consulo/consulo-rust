/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.openapiext;

import consulo.application.ApplicationManager;
import consulo.application.progress.Task;
import consulo.application.progress.TaskInfo;
import consulo.application.impl.internal.progress.ProgressWindow;
import consulo.project.ui.wm.IdeFrame;
import consulo.project.ui.internal.StatusBarEx;
import consulo.project.ui.internal.WindowManagerEx;
import consulo.ui.ex.awt.util.TimerUtil;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import javax.swing.*;

/**
 * Like {@link com.intellij.openapi.progress.impl.BackgroundableProcessIndicator},
 * but allows to specify delay to postpone progress bar displaying in UI.
 */
public class DelayedBackgroundableProcessIndicator extends ProgressWindow {

    private final Task.Backgroundable myTask;
    @Nullable
    private StatusBarEx myStatusBar;
    private boolean myDidInitializeOnEdt;
    private boolean myIsDisposed;
    private volatile boolean myIsFinishCalled;

    public DelayedBackgroundableProcessIndicator(@Nonnull Task.Backgroundable task, int delay) {
        super(task.isCancellable(), true, (consulo.project.Project) task.getProject(), null, task.getCancelTextValue());
        myTask = task;
        setOwnerTask(task);
        initializeStatusBar();

        Timer timer = TimerUtil.createNamedTimer("DelayedBackgroundableProcessIndicator timer", delay, e -> {
            ApplicationManager.getApplication().invokeLater(() -> {
                if (isRunning() && !myIsFinishCalled && !myIsDisposed && !myBackgrounded) {
                    background();
                }
            }, getModalityState());
        });
        timer.setRepeats(false);
        timer.start();
    }

    @Nonnull
    public Task.Backgroundable getTask() {
        return myTask;
    }

    private void initializeStatusBar() {
        if (myIsDisposed || myDidInitializeOnEdt) return;
        myDidInitializeOnEdt = true;
        setTitle(myTask.getTitle());
        if (myStatusBar == null) {
            consulo.project.Project project = (consulo.project.Project) myTask.getProject();
            consulo.project.Project nonDefaultProject =
                (project == null || project.isDisposed() || project.isDefault()) ? null : project;
            IdeFrame frame = WindowManagerEx.getInstanceEx().findFrameFor(nonDefaultProject);
            myStatusBar = frame != null ? (StatusBarEx) frame.getStatusBar() : null;
        }
    }

    @Override
    public void background() {
        if (myIsDisposed) return;
        assert myDidInitializeOnEdt : "Call to background action before showing dialog";
        myTask.processSentToBackground();
        doBackground(myStatusBar);
        super.background();
    }

    private void doBackground(@Nullable StatusBarEx statusBar) {
        if (statusBar != null) {
            statusBar.addProgress(this, myTask);
        }
    }

    @Override
    protected void prepareShowDialog() {
        // Don't show the modal window
    }

    @Override
    public void showDialog() {
        // Don't show the modal window
    }

    @Override
    public void finish(@Nonnull TaskInfo task) {
        myIsFinishCalled = true;
        super.finish(task);
    }

    @Override
    public void dispose() {
        super.dispose();
        myIsDisposed = true;
    }
}
