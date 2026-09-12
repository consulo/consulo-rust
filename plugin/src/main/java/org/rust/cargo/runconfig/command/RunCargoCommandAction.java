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
import consulo.rust.localize.RustLocalize;
import consulo.localize.LocalizeValue;
import consulo.platform.base.icon.PlatformIconGroup;

@ActionImpl(id = "Cargo.RunCargoCommand")
public class RunCargoCommandAction extends RunCargoCommandActionBase {

    public RunCargoCommandAction() {
        super(RustLocalize.actionCargoRuncargocommandText(), LocalizeValue.empty(), PlatformIconGroup.actionsExecute());
    }
    @Override
    public void actionPerformed(@Nonnull AnActionEvent e) {
        Project project = e.getData(Project.KEY);
        if (project == null) return;
        RunAnythingManager runAnythingManager = RunAnythingManager.getInstance(project);
        runAnythingManager.show(CargoRunAnythingProvider.HELP_COMMAND + " ", false, e);
    }
}
