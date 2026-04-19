/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.actions;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.Project;
import org.rust.cargo.project.model.CargoProjectActionBase;
import org.rust.cargo.project.model.CargoProjectServiceUtil;
import org.rust.cargo.project.settings.RsProjectSettingsServiceUtil;
import org.rust.openapiext.OpenApiUtil;

public class RefreshCargoProjectsAction extends CargoProjectActionBase {

    @Override
    public void update(AnActionEvent e) {
        Project project = e.getProject();
        e.getPresentation().setEnabled(
            project != null &&
            RsProjectSettingsServiceUtil.getToolchain(project) != null &&
            org.rust.cargo.runconfig.RunConfigUtil.hasCargoProject(project)
        );
    }

    @Override
    public void actionPerformed(AnActionEvent e) {
        Project project = e.getProject();
        if (project == null) return;

        if (!org.rust.ide.notifications.NotificationUtils.confirmLoadingUntrustedProject(project)) return;

        OpenApiUtil.saveAllDocuments();
        if (RsProjectSettingsServiceUtil.getToolchain(project) == null || !org.rust.cargo.runconfig.RunConfigUtil.hasCargoProject(project)) {
            CargoProjectServiceUtil.guessAndSetupRustProject(project, true);
        } else {
            CargoProjectServiceUtil.getCargoProjects(project).refreshAllProjects();
        }
    }
}
