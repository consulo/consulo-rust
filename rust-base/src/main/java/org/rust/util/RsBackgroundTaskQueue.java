/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.util;

import consulo.application.Application;
import consulo.application.progress.DumbModeAction;
import consulo.application.progress.ProgressIndicator;
import consulo.application.progress.Task;
import consulo.application.util.BackgroundTaskQueue;
import consulo.component.ComponentManager;
import consulo.localize.LocalizeValue;
import consulo.logging.Logger;
import consulo.project.DumbService;
import consulo.project.Project;
import consulo.ui.ModalityState;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.rust.RsTask;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Runs backgroundable tasks one by one, adding the policy {@link RsTask} asks for: a newly submitted
 * task can cancel queued or running tasks of a weaker type, a task can hold its slot until the project
 * leaves dumb mode, and a task can ask to run in the calling thread under tests.
 * <p>
 * The queue itself, the progress indicator and the unit-test path come from the platform's
 * {@link BackgroundTaskQueue}; only the policy above lives here. The port used to drive
 * {@code ProgressManagerImpl} directly, which is an ide-impl internal and is not the registered
 * progress manager outside the full IDE - so the queue simply failed in a headless application.
 */
public class RsBackgroundTaskQueue extends BackgroundTaskQueue {
    private static final Logger LOG = Logger.getInstance(RsBackgroundTaskQueue.class);

    private final List<QueuedTask> myCancelableTasks = new ArrayList<>();
    private volatile boolean myIsDisposed = false;

    public RsBackgroundTaskQueue(@Nonnull Application application, @Nullable ComponentManager owner) {
        super(application, owner, "Rust");
    }

    @Override
    public void run(Task.Backgroundable task, @Nullable ModalityState modalityState, @Nullable ProgressIndicator indicator) {
        if (myIsDisposed) {
            return;
        }

        LOG.debug("Scheduling task " + task);

        QueuedTask queued;
        synchronized (this) {
            if (task instanceof RsTask rsTask) {
                cancelTasks(rsTask.getTaskType());
            }
            queued = new QueuedTask(task, this::onFinish);
            myCancelableTasks.add(queued);
        }

        super.run(queued, modalityState, indicator);
    }

    /** Equivalent to submitting an empty task of {@code taskType}: everything it outranks is canceled. */
    public synchronized void cancelTasks(@Nonnull RsTask.TaskType taskType) {
        myCancelableTasks.removeIf(queued -> {
            if (queued.getDelegate() instanceof RsTask other && taskType.canCancelOther(other.getTaskType())) {
                queued.cancel();
                return true;
            }
            return false;
        });
    }

    private synchronized void onFinish(@Nonnull QueuedTask queued) {
        myCancelableTasks.remove(queued);
    }

    public void dispose() {
        myIsDisposed = true;
        clear();
        cancelAll();
    }

    private synchronized void cancelAll() {
        for (QueuedTask queued : myCancelableTasks) {
            queued.cancel();
        }
        myCancelableTasks.clear();
    }

    /**
     * Wraps a queued task so the queue can reach the progress indicator the platform hands it - that
     * indicator is what {@link #cancelTasks} cancels. A task canceled before it starts never reaches
     * the delegate at all.
     * <p>
     * Every callback is forwarded, so the delegate cannot tell it was wrapped. Only the parent
     * component is not carried over, which no backgroundable task in this plugin sets.
     */
    private static class QueuedTask extends Task.Backgroundable {
        private final Task.Backgroundable myDelegate;
        private final Consumer<QueuedTask> myOnFinish;

        private @Nullable ProgressIndicator myIndicator;
        private boolean myCanceled = false;

        QueuedTask(@Nonnull Task.Backgroundable delegate, @Nonnull Consumer<QueuedTask> onFinish) {
            super(delegate.getProject(), LocalizeValue.of(delegate.getTitle()), delegate.isCancellable());
            myDelegate = delegate;
            myOnFinish = onFinish;
        }

        @Nonnull
        Task.Backgroundable getDelegate() {
            return myDelegate;
        }

        synchronized void cancel() {
            myCanceled = true;
            if (myIndicator != null) {
                myIndicator.cancel();
            }
        }

        @Override
        public void run(ProgressIndicator indicator) {
            synchronized (this) {
                if (myCanceled) {
                    return;
                }
                myIndicator = indicator;
            }

            // holds the queue slot while waiting, which is the ordering the task asked for
            if (myDelegate instanceof RsTask rsTask
                && rsTask.getWaitForSmartMode()
                && myDelegate.getProject() instanceof Project project) {
                DumbService.getInstance(project).waitForSmartMode();
                indicator.checkCanceled();
            }

            myDelegate.run(indicator);
        }

        @Override
        public void onCancel() {
            myDelegate.onCancel();
        }

        @Override
        public void onSuccess() {
            myDelegate.onSuccess();
        }

        @Override
        public void onThrowable(Throwable throwable) {
            myDelegate.onThrowable(throwable);
        }

        @Override
        public void onFinished() {
            myOnFinish.accept(this);
            myDelegate.onFinished();
        }

        @Override
        public @Nullable NotificationInfo getNotificationInfo() {
            return myDelegate.getNotificationInfo();
        }

        @Override
        public @Nullable NotificationInfo notifyFinished() {
            return myDelegate.notifyFinished();
        }

        @Override
        public LocalizeValue getCancelTextValue() {
            return myDelegate.getCancelTextValue();
        }

        @Override
        public LocalizeValue getCancelTooltipTextValue() {
            return myDelegate.getCancelTooltipTextValue();
        }

        @Override
        public boolean shouldStartInBackground() {
            return myDelegate.shouldStartInBackground();
        }

        @Override
        public void processSentToBackground() {
            myDelegate.processSentToBackground();
        }

        @Override
        public boolean isConditionalModal() {
            return myDelegate.isConditionalModal();
        }

        @Override
        @SuppressWarnings("deprecation")
        public DumbModeAction getDumbModeAction() {
            return myDelegate.getDumbModeAction();
        }

        @Override
        @SuppressWarnings("deprecation")
        public boolean isHeadless() {
            return myDelegate.isHeadless();
        }

        @Override
        public String toString() {
            return myDelegate.toString();
        }
    }
}
