/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.actions;

import com.intellij.notification.Notification;
import com.intellij.notification.NotificationType;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.DumbAwareAction;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.rust.RsBundle;
import org.rust.cargo.project.settings.RsProjectSettingsServiceUtil;
import org.rust.cargo.toolchain.RsToolchainBase;
import org.rust.cargo.toolchain.tools.Rustup;
import org.rust.cargo.util.DownloadResult;
import org.rust.ide.notifications.NotificationUtils;

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
        Project project = e.getProject();
        if (project == null) return;
        RsToolchainBase toolchain = RsProjectSettingsServiceUtil.getToolchain(project);
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
            public void run(@NotNull ProgressIndicator indicator) {
                DownloadResult result = rustup.downloadTarget(project, targetName);
                if (result instanceof DownloadResult.Err err) {
                    NotificationUtils.showBalloon(project, err.getError(), NotificationType.ERROR, null);
                }
            }
        }.queue();
    }
}
