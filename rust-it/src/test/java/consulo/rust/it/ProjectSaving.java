/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package consulo.rust.it;

import consulo.application.Application;
import consulo.it.HeadlessProjects;
import consulo.project.Project;
import consulo.util.concurrent.coroutine.CoroutineScope;
import consulo.virtualFileSystem.RefreshQueue;
import consulo.virtualFileSystem.VirtualFile;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/** Saving a project before a test closes and reopens it. */
final class ProjectSaving {
    private ProjectSaving() {
    }

    /**
     * Saves the project and waits for the refresh that picks up what the save created.
     * <p>
     * Saving creates the project directory, and that refresh scans it with the project's file index. Closing the project
     * under the scan fails it, so the save returns only after it: the refresh sessions run one after another, so one
     * queued after it finishes after it.
     *
     * @see consulo.it.index.ScanningTestSupport in the platform, which the save mirrors.
     */
    @SuppressWarnings("deprecation")
    static void save(Project project, Application application) throws Exception {
        project.saveAsync(application.getLastUIAccess())
            .runAsync(CoroutineScope.of(project.coroutineContext()), null)
            .toFuture()
            .get(HeadlessProjects.TIMEOUT_SECONDS, TimeUnit.SECONDS);

        VirtualFile baseDir = project.getBaseDir();
        if (baseDir == null) return;
        CompletableFuture<Void> refreshed = new CompletableFuture<>();
        RefreshQueue.getInstance().refresh(true, true, () -> refreshed.complete(null), baseDir);
        refreshed.get(HeadlessProjects.TIMEOUT_SECONDS, TimeUnit.SECONDS);
    }
}
