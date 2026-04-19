/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.cargo.runconfig.command;

import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.DumbAwareAction;
import org.jetbrains.annotations.NotNull;
import org.rust.cargo.runconfig.RunConfigUtil;

public abstract class RunCargoCommandActionBase extends DumbAwareAction {
    @NotNull
    @Override
    public ActionUpdateThread getActionUpdateThread() {
        return ActionUpdateThread.BGT;
    }

    @Override
    public void update(@NotNull AnActionEvent e) {
        boolean hasCargoProject = e.getProject() != null && RunConfigUtil.hasCargoProject(e.getProject());
        e.getPresentation().setEnabledAndVisible(hasCargoProject);
    }
}
