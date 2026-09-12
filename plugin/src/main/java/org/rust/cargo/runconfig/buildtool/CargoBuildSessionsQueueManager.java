/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.cargo.runconfig.buildtool;

import consulo.application.util.BackgroundTaskQueue;
import consulo.project.Project;
import org.rust.RsBundle;
import consulo.annotation.component.ServiceAPI;
import consulo.annotation.component.ServiceImpl;
import consulo.annotation.component.ComponentScope;
import jakarta.inject.Inject;
import consulo.application.Application;

@ServiceAPI(ComponentScope.PROJECT)
@ServiceImpl
public final class CargoBuildSessionsQueueManager {
    private final BackgroundTaskQueue buildSessionsQueue;

    @Inject

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
