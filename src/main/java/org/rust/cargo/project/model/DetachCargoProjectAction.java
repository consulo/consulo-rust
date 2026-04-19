/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.cargo.project.model;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.rust.cargo.project.toolwindow.CargoToolWindow;

public class DetachCargoProjectAction extends CargoProjectActionBase {

    @Override
    public void update(@NotNull AnActionEvent e) {
        e.getPresentation().setEnabled(e.getProject() != null && getCargoProject(e) != null);
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        Project project = e.getProject();
        if (project == null) return;
        CargoProject cargoProject = getCargoProject(e);
        if (cargoProject == null) return;
        CargoProjectServiceUtil.getCargoProjects(project).detachCargoProject(cargoProject);
    }

    private CargoProject getCargoProject(@NotNull AnActionEvent e) {
        return e.getData(CargoToolWindow.SELECTED_CARGO_PROJECT);
    }
}
