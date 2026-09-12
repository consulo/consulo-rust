/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.actions;

import consulo.ui.ex.action.AnAction;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ide.setting.ShowSettingsUtil;
import consulo.application.dumb.DumbAware;
import consulo.ui.ex.action.LegacyAnAction;
import org.rust.cargo.project.configurable.RustfmtConfigurable;
import consulo.project.Project;

public class RustfmtEditSettingsAction extends LegacyAnAction implements DumbAware {

    public RustfmtEditSettingsAction(String text) {
        super(text);
    }

    @Override
    public void update(AnActionEvent e) {
        super.update(e);
        e.getPresentation().setEnabledAndVisible(e.getData(consulo.project.Project.KEY) != null);
    }

    @Override
    public void actionPerformed(AnActionEvent e) {
        ShowSettingsUtil.getInstance().showSettingsDialog(e.getData(consulo.project.Project.KEY), RustfmtConfigurable.class);
    }
}
