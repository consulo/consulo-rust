/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.actions;

import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.action.DefaultActionGroup;
import consulo.application.dumb.DumbAware;
import consulo.project.Project;
import org.rust.cargo.runconfig.RunConfigUtil;

public class RsToolsActionGroup extends DefaultActionGroup implements DumbAware {


    @Override
    public void update(AnActionEvent e) {
        Project project = e.getData(consulo.project.Project.KEY);
        if (project == null) return;
        e.getPresentation().setEnabledAndVisible(RunConfigUtil.hasCargoProject(project));
    }
}
