/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.actions;

import org.rust.cargo.toolchain.RsToolchainLocator;
import consulo.ui.ex.action.AnActionEvent;
import consulo.project.Project;
import org.rust.cargo.project.model.CargoProjectActionBase;
import org.rust.cargo.project.model.CargoProjectServiceUtil;
import org.rust.cargo.api.settings.RsProjectSettingsServiceUtil;
import org.rust.openapiext.OpenApiUtil;
import consulo.annotation.component.ActionImpl;
import org.rust.cargo.runconfig.RunConfigUtil;
import org.rust.notifications.NotificationUtils;
import consulo.rust.localize.RustLocalize;
import consulo.localize.LocalizeValue;
import consulo.rust.icon.RustIconGroup;

@ActionImpl(id = "Cargo.RefreshCargoProject")
public class RefreshCargoProjectsAction extends CargoProjectActionBase {

    public RefreshCargoProjectsAction() {
        super(RustLocalize.actionCargoRefreshcargoprojectText(), RustLocalize.actionCargoRefreshcargoprojectDescription(), RustIconGroup.rustreload());
    }

    @Override
    public void update(AnActionEvent e) {
        Project project = e.getData(consulo.project.Project.KEY);
        e.getPresentation().setEnabled(
            project != null &&
            RsToolchainLocator.getToolchain(project) != null &&
            org.rust.cargo.runconfig.RunConfigUtil.hasCargoProject(project)
        );
    }

    @Override
    public void actionPerformed(AnActionEvent e) {
        Project project = e.getData(consulo.project.Project.KEY);
        if (project == null) return;

        OpenApiUtil.saveAllDocuments();
        if (RsToolchainLocator.getToolchain(project) == null || !org.rust.cargo.runconfig.RunConfigUtil.hasCargoProject(project)) {
            CargoProjectServiceUtil.guessAndSetupRustProject(project, true);
        } else {
            CargoProjectServiceUtil.getCargoProjects(project).refreshAllProjects();
        }
    }
}
