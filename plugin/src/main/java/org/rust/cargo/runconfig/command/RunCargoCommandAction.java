/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.cargo.runconfig.command;

import consulo.ide.impl.idea.ide.actions.runAnything.RunAnythingManager;
import consulo.ui.ex.action.AnActionEvent;
import consulo.project.Project;
import jakarta.annotation.Nonnull;
import org.rust.ide.actions.runAnything.cargo.CargoRunAnythingProvider;
import consulo.annotation.component.ActionImpl;

@ActionImpl(id = "Cargo.RunCargoCommand")
public class RunCargoCommandAction extends RunCargoCommandActionBase {
    @Override
    public void actionPerformed(@Nonnull AnActionEvent e) {
        Project project = e.getData(Project.KEY);
        if (project == null) return;
        RunAnythingManager runAnythingManager = RunAnythingManager.getInstance(project);
        runAnythingManager.show(CargoRunAnythingProvider.HELP_COMMAND + " ", false, e);
    }
}
