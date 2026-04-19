/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust;

import com.intellij.openapi.Disposable;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import org.rust.util.RsBackgroundTaskQueue;

/**
 * A common queue for cargo and macro expansion tasks that should be executed sequentially.
 * Can run any {@link Task.Backgroundable}, but provides additional features for tasks that implement {@link RsTask}.
 * The most important feature is that newly submitted tasks can cancel a currently running task or
 * tasks in the queue (See {@link RsTask#getTaskType()}).
 */
@Service
public final class RsProjectTaskQueueService implements Disposable {
    private final RsBackgroundTaskQueue queue = new RsBackgroundTaskQueue();

    /** Submits a task. A task can implement {@link RsTask} */
    public void run(Task.Backgroundable task) {
        queue.run(task);
    }

    /** Equivalent to running an empty task with {@link RsTask#getTaskType()} = {@code taskType} */
    public void cancelTasks(RsTask.TaskType taskType) {
        queue.cancelTasks(taskType);
    }

    /** @return true if no running or pending tasks */
    public boolean isEmpty() {
        return queue.isEmpty();
    }

    @Override
    public void dispose() {
        queue.dispose();
    }

    public static RsProjectTaskQueueService getInstance(Project project) {
        return project.getService(RsProjectTaskQueueService.class);
    }
}
