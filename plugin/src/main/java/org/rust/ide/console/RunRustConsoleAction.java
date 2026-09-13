/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.console;

import consulo.project.ui.notification.NotificationType;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.action.DumbAwareAction;
import consulo.project.Project;
import consulo.ui.ex.action.LegacyDumbAwareAction;
import jakarta.annotation.Nonnull;
import org.rust.RsBundle;
import org.rust.cargo.runconfig.RsRunConfigurationUtil;
import org.rust.notifications.NotificationUtils;
import consulo.annotation.component.ActionImpl;

@ActionImpl(id = "Rust.ConsoleREPL")
public class RunRustConsoleAction extends LegacyDumbAwareAction {


    @Override
    public void update(@Nonnull AnActionEvent e) {
        Project project = e.getData(consulo.project.Project.KEY);
        e.getPresentation().setEnabled(project != null && RsRunConfigurationUtil.hasCargoProject(project));
    }

    @Override
    public void actionPerformed(@Nonnull AnActionEvent e) {
        Project project = e.getData(consulo.project.Project.KEY);
        if (project == null) {
            NotificationUtils.showBalloonWithoutProject(
                RsBundle.message("notification.content.project.not.found"), NotificationType.ERROR);
            return;
        }

        RsConsoleRunner runner = new RsConsoleRunner(project);
        project.getApplication().invokeLater(() -> runner.runSync(true), project.getDisposed());
    }
}
