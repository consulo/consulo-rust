/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.actions;

import consulo.project.ui.notification.Notification;
import consulo.project.ui.notification.NotificationType;
import consulo.ui.ex.action.AnActionEvent;
import consulo.application.progress.ProgressIndicator;
import consulo.application.progress.Task;
import consulo.ui.ex.action.DumbAwareAction;
import consulo.project.Project;
import jakarta.annotation.Nonnull;
import org.rust.RsBundle;
import org.rust.cargo.project.settings.RsProjectSettingsServiceUtil;
import org.rust.cargo.toolchain.RsToolchainBase;
import org.rust.cargo.toolchain.tools.Rustup;
import org.rust.cargo.util.DownloadResult;
import org.rust.ide.notifications.NotificationUtils;

import java.nio.file.Path;

public class InstallComponentAction extends DumbAwareAction {
    private final Path projectDirectory;
    private final String componentName;

    public InstallComponentAction(Path projectDirectory, String componentName) {
        super(RsBundle.message("action.install.text"));
        this.projectDirectory = projectDirectory;
        this.componentName = componentName;
    }

    @Override
    public void actionPerformed(AnActionEvent e) {
        Project project = e.getData(consulo.project.Project.KEY);
        if (project == null) return;
        RsToolchainBase toolchain = RsProjectSettingsServiceUtil.getToolchain(project);
        if (toolchain == null) return;
        Rustup rustup = Rustup.create(toolchain, projectDirectory);
        if (rustup == null) return;
        Notification.get(e).expire();
        new Task.Backgroundable(project, RsBundle.message("progress.title.installing2", componentName)) {
            @Override
            public boolean shouldStartInBackground() {
                return false;
            }

            @Override
            public void run(@Nonnull ProgressIndicator indicator) {
                DownloadResult result = rustup.downloadComponent(project, componentName);
                if (result instanceof DownloadResult.Err err) {
                    NotificationUtils.showBalloon(project, err.getError(), NotificationType.ERROR, null);
                }
            }
        }.queue();
    }
}
