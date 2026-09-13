/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.cargo.toolchain.actions;

import org.rust.cargo.toolchain.RsToolchainLocator;
import consulo.project.ui.notification.Notification;
import consulo.project.ui.notification.NotificationType;
import consulo.ui.ex.action.AnActionEvent;
import consulo.application.progress.ProgressIndicator;
import consulo.application.progress.Task;
import consulo.ui.ex.action.DumbAwareAction;
import consulo.project.Project;
import jakarta.annotation.Nonnull;
import org.rust.RsBundle;
import org.rust.cargo.api.settings.RsProjectSettingsServiceUtil;
import org.rust.cargo.toolchain.RsToolchainBase;
import org.rust.cargo.toolchain.tools.Rustup;
import org.rust.cargo.util.DownloadResult;
import org.rust.notifications.NotificationUtils;

import java.nio.file.Path;

public class InstallTargetAction extends DumbAwareAction {
    private final Path projectDirectory;
    private final String targetName;

    public InstallTargetAction(Path projectDirectory, String targetName) {
        super(RsBundle.message("action.install.text"));
        this.projectDirectory = projectDirectory;
        this.targetName = targetName;
    }

    @Override
    public void actionPerformed(AnActionEvent e) {
        Project project = e.getData(consulo.project.Project.KEY);
        if (project == null) return;
        RsToolchainBase toolchain = RsToolchainLocator.getToolchain(project);
        if (toolchain == null) return;
        Rustup rustup = Rustup.create(toolchain, projectDirectory);
        if (rustup == null) return;

        Notification.get(e).expire();

        new Task.Backgroundable(project, RsBundle.message("progress.title.installing", targetName)) {
            @Override
            public boolean shouldStartInBackground() {
                return false;
            }

            @Override
            public void run(@Nonnull ProgressIndicator indicator) {
                DownloadResult result = rustup.downloadTarget(project, targetName);
                if (result instanceof DownloadResult.Err err) {
                    NotificationUtils.showBalloon(project, err.getError(), NotificationType.ERROR, null);
                }
            }
        }.queue();
    }
}
