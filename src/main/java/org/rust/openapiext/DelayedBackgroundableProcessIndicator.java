/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.openapiext;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.progress.TaskInfo;
import com.intellij.openapi.progress.util.ProgressWindow;
import com.intellij.openapi.wm.IdeFrame;
import com.intellij.openapi.wm.ex.StatusBarEx;
import com.intellij.openapi.wm.ex.WindowManagerEx;
import com.intellij.util.ui.TimerUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

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

    public DelayedBackgroundableProcessIndicator(@NotNull Task.Backgroundable task, int delay) {
        super(task.isCancellable(), true, task.getProject(), null, task.getCancelText());
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

    @NotNull
    public Task.Backgroundable getTask() {
        return myTask;
    }

    private void initializeStatusBar() {
        if (myIsDisposed || myDidInitializeOnEdt) return;
        myDidInitializeOnEdt = true;
        setTitle(myTask.getTitle());
        if (myStatusBar == null) {
            com.intellij.openapi.project.Project nonDefaultProject =
                (myTask.getProject() == null || myTask.getProject().isDisposed() || myTask.getProject().isDefault())
                    ? null : myTask.getProject();
            IdeFrame frame = WindowManagerEx.getInstanceEx().findFrameHelper(nonDefaultProject);
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
    public void finish(@NotNull TaskInfo task) {
        myIsFinishCalled = true;
        super.finish(task);
    }

    @Override
    public void dispose() {
        super.dispose();
        myIsDisposed = true;
    }
}
