/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.actions;

import consulo.ui.ex.action.AnAction;
import consulo.ui.ex.action.AnActionEvent;
import consulo.application.dumb.DumbAware;
import consulo.project.Project;
import consulo.ide.setting.ShowSettingsUtil;
import consulo.ui.ex.action.LegacyAnAction;
import org.rust.cargo.project.configurable.CargoConfigurable;
import consulo.annotation.component.ActionImpl;

@ActionImpl(id = "Cargo.ShowSettings")
public class CargoEditSettingsAction extends LegacyAnAction implements DumbAware {


    @Override
    public void update(AnActionEvent e) {
        super.update(e);
        e.getPresentation().setEnabledAndVisible(e.getData(consulo.project.Project.KEY) != null);
    }

    @Override
    public void actionPerformed(AnActionEvent e) {
        Project project = e.getData(consulo.project.Project.KEY);
        if (project != null) {
            ShowSettingsUtil.getInstance().showSettingsDialog(project, CargoConfigurable.class);
        }
    }
}
