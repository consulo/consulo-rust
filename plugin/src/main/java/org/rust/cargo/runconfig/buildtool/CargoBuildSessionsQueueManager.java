/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.cargo.runconfig.buildtool;

import com.intellij.openapi.components.Service;
import consulo.application.util.BackgroundTaskQueue;
import consulo.project.Project;
import org.rust.RsBundle;

@Service
public final class CargoBuildSessionsQueueManager {
    private final BackgroundTaskQueue buildSessionsQueue;

    public CargoBuildSessionsQueueManager(Project project) {
        this.buildSessionsQueue = new BackgroundTaskQueue(consulo.application.Application.get(), project, RsBundle.message("progress.title.building"));
    }

    public BackgroundTaskQueue getBuildSessionsQueue() {
        return buildSessionsQueue;
    }

    public static CargoBuildSessionsQueueManager getInstance(Project project) {
        return project.getService(CargoBuildSessionsQueueManager.class);
    }
}
