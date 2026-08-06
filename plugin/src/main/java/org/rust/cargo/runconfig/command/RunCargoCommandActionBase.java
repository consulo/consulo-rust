/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.cargo.runconfig.command;
import consulo.project.Project;

import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.action.DumbAwareAction;
import jakarta.annotation.Nonnull;
import org.rust.cargo.runconfig.RunConfigUtil;

public abstract class RunCargoCommandActionBase extends DumbAwareAction {

    @Override
    public void update(@Nonnull AnActionEvent e) {
        boolean hasCargoProject = e.getData(Project.KEY) != null && RunConfigUtil.hasCargoProject(e.getData(Project.KEY));
        e.getPresentation().setEnabledAndVisible(hasCargoProject);
    }
}
