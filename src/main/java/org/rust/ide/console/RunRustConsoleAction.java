/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.console;

import com.intellij.notification.NotificationType;
import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.application.TransactionGuard;
import com.intellij.openapi.project.DumbAwareAction;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.rust.RsBundle;
import org.rust.cargo.runconfig.RsRunConfigurationUtil;
import org.rust.ide.notifications.NotificationUtils;

public class RunRustConsoleAction extends DumbAwareAction {

    @Override
    @NotNull
    public ActionUpdateThread getActionUpdateThread() {
        return ActionUpdateThread.BGT;
    }

    @Override
    public void update(@NotNull AnActionEvent e) {
        Project project = e.getProject();
        e.getPresentation().setEnabled(project != null && RsRunConfigurationUtil.hasCargoProject(project));
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        Project project = e.getProject();
        if (project == null) {
            NotificationUtils.showBalloonWithoutProject(
                RsBundle.message("notification.content.project.not.found"), NotificationType.ERROR);
            return;
        }

        RsConsoleRunner runner = new RsConsoleRunner(project);
        //noinspection deprecation
        TransactionGuard.submitTransaction(project, () -> runner.runSync(true));
    }
}
