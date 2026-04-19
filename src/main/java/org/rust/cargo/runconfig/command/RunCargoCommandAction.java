/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.cargo.runconfig.command;

import com.intellij.ide.actions.runAnything.RunAnythingManager;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.rust.ide.actions.runAnything.cargo.CargoRunAnythingProvider;

public class RunCargoCommandAction extends RunCargoCommandActionBase {
    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        Project project = e.getProject();
        if (project == null) return;
        RunAnythingManager runAnythingManager = RunAnythingManager.getInstance(project);
        runAnythingManager.show(CargoRunAnythingProvider.HELP_COMMAND + " ", false, e);
    }
}
