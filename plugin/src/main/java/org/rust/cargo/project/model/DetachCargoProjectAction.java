/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.cargo.project.model;

import consulo.ui.ex.action.AnActionEvent;
import consulo.project.Project;
import jakarta.annotation.Nonnull;
import org.rust.cargo.project.toolwindow.CargoToolWindow;
import consulo.annotation.component.ActionImpl;
import consulo.annotation.component.ActionRef;

@ActionImpl(
    id = "Cargo.DetachCargoProject",
    shortcutFrom = @ActionRef(id = "$Delete")
)
public class DetachCargoProjectAction extends CargoProjectActionBase {

    @Override
    public void update(@Nonnull AnActionEvent e) {
        e.getPresentation().setEnabled(e.getData(Project.KEY) != null && getCargoProject(e) != null);
    }

    @Override
    public void actionPerformed(@Nonnull AnActionEvent e) {
        Project project = e.getData(Project.KEY);
        if (project == null) return;
        CargoProject cargoProject = getCargoProject(e);
        if (cargoProject == null) return;
        CargoProjectServiceUtil.getCargoProjects(project).detachCargoProject(cargoProject);
    }

    private CargoProject getCargoProject(@Nonnull AnActionEvent e) {
        return e.getData(CargoToolWindow.SELECTED_CARGO_PROJECT);
    }
}
