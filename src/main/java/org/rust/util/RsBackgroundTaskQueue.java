/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.util;

import com.intellij.openapi.application.ModalityState;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.progress.EmptyProgressIndicator;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.progress.impl.BackgroundableProcessIndicator;
import com.intellij.openapi.progress.impl.ProgressManagerImpl;
import com.intellij.openapi.project.DumbService;
import com.intellij.util.concurrency.QueueProcessor;
import org.jetbrains.annotations.NotNull;
import org.rust.RsTask;
import org.rust.openapiext.DelayedBackgroundableProcessIndicator;
import org.rust.openapiext.OpenApiUtil;

import com.intellij.util.PairConsumer;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class RsBackgroundTaskQueue {
    private static final Logger LOG = Logger.getInstance(RsBackgroundTaskQueue.class);

    private final QueueProcessor<ContinuableRunnable> myProcessor;
    private volatile boolean myIsDisposed = false;
    private final List<BackgroundableTaskData> myCancelableTasks = new ArrayList<>();

    public RsBackgroundTaskQueue() {
        myProcessor = new QueueProcessor<>(
            new QueueConsumer(),
            true,
            QueueProcessor.ThreadToUse.AWT,
            (com.intellij.openapi.util.Condition<Object>) o -> myIsDisposed
        );
    }

    public boolean isEmpty() {
        return myProcessor.isEmpty();
    }

    public synchronized void run(@NotNull Task.Backgroundable task) {
        if (OpenApiUtil.isUnitTestMode() && task instanceof RsTask && ((RsTask) task).getRunSyncInUnitTests()) {
            runTaskInCurrentThread(task);
        } else {
            LOG.debug("Scheduling task " + task);
            if (task instanceof RsTask) {
                cancelTasks(((RsTask) task).getTaskType());
            }
            BackgroundableTaskData data = new BackgroundableTaskData(task, this::onFinish);
            myCancelableTasks.add(data);
            myProcessor.add(data);
        }
    }

    private void runTaskInCurrentThread(@NotNull Task.Backgroundable task) {
        ProgressManagerImpl pm = (ProgressManagerImpl) ProgressManager.getInstance();
        pm.runProcessWithProgressInCurrentThread(task, new EmptyProgressIndicator(), ModalityState.NON_MODAL);
    }

    public synchronized void cancelTasks(@NotNull RsTask.TaskType taskType) {
        myCancelableTasks.removeIf(data -> {
            if (data.getTask() instanceof RsTask && taskType.canCancelOther(((RsTask) data.getTask()).getTaskType())) {
                data.cancel();
                return true;
            }
            return false;
        });
    }

    private synchronized void onFinish(@NotNull BackgroundableTaskData data) {
        myCancelableTasks.remove(data);
    }

    public void dispose() {
        myIsDisposed = true;
        myProcessor.clear();
        cancelAll();
    }

    private synchronized void cancelAll() {
        for (BackgroundableTaskData task : myCancelableTasks) {
            task.cancel();
        }
        myCancelableTasks.clear();
    }

    private interface ContinuableRunnable {
        void run(@NotNull Runnable continuation);
    }

    private static class QueueConsumer implements PairConsumer<ContinuableRunnable, Runnable> {
        @Override
        public void consume(ContinuableRunnable t, Runnable u) {
            t.run(u);
        }
    }

    private static class BackgroundableTaskData implements ContinuableRunnable {
        private final Task.Backgroundable myTask;
        private final Consumer<BackgroundableTaskData> myOnFinish;
        private State myState = State.PENDING;
        private ProgressIndicator myIndicator;
        private Runnable myContinuation;

        BackgroundableTaskData(@NotNull Task.Backgroundable task, @NotNull Consumer<BackgroundableTaskData> onFinish) {
            this.myTask = task;
            this.myOnFinish = onFinish;
        }

        @NotNull
        public Task.Backgroundable getTask() {
            return myTask;
        }

        @Override
        public synchronized void run(@NotNull Runnable continuation) {
            OpenApiUtil.checkIsDispatchThread();

            switch (myState) {
                case CANCELED_CONTINUED:
                    return;
                case CANCELED:
                    continuation.run();
                    return;
                case RUNNING:
                    throw new IllegalStateException("Trying to re-run already running task");
            }

            if (myTask instanceof RsTask && ((RsTask) myTask).getWaitForSmartMode()
                && DumbService.isDumb(myTask.getProject())) {
                myState = State.WAIT_FOR_SMART_MODE;
                myContinuation = continuation;
                DumbService.getInstance(myTask.getProject()).runWhenSmart(() -> run(continuation));
                return;
            }

            ProgressIndicator indicator;
            if (OpenApiUtil.isHeadlessEnvironment()) {
                indicator = new EmptyProgressIndicator();
            } else if (myTask instanceof RsTask && ((RsTask) myTask).getProgressBarShowDelay() > 0) {
                indicator = new DelayedBackgroundableProcessIndicator(myTask, ((RsTask) myTask).getProgressBarShowDelay());
            } else {
                indicator = new BackgroundableProcessIndicator(myTask);
            }

            myState = State.RUNNING;
            myIndicator = indicator;

            ProgressManagerImpl pm = (ProgressManagerImpl) ProgressManager.getInstance();
            pm.runProcessWithProgressAsynchronously(
                myTask,
                indicator,
                () -> {
                    myOnFinish.accept(this);
                    continuation.run();
                },
                ModalityState.NON_MODAL
            );
        }

        public synchronized void cancel() {
            switch (myState) {
                case PENDING:
                    myState = State.CANCELED;
                    break;
                case RUNNING:
                    if (myIndicator != null) {
                        myIndicator.cancel();
                    }
                    break;
                case WAIT_FOR_SMART_MODE:
                    myState = State.CANCELED_CONTINUED;
                    if (myContinuation != null) {
                        myContinuation.run();
                    }
                    break;
                case CANCELED:
                case CANCELED_CONTINUED:
                    break;
            }
        }

        private enum State {
            PENDING, WAIT_FOR_SMART_MODE, CANCELED, CANCELED_CONTINUED, RUNNING
        }
    }
}
