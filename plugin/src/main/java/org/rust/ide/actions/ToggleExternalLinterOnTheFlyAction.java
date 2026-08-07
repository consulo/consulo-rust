/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.actions;

import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.action.ToggleAction;
import consulo.project.Project;
import org.rust.RsBundle;
import org.rust.cargo.project.settings.RsExternalLinterProjectSettingsService;
import org.rust.cargo.project.settings.RsProjectSettingsServiceUtil;
import consulo.annotation.component.ActionImpl;

@ActionImpl(id = "Cargo.ToggleExternalLinterOnTheFlyAction")
public class ToggleExternalLinterOnTheFlyAction extends ToggleAction {

    @Override
    public boolean isSelected(AnActionEvent e) {
        Project project = e.getData(consulo.project.Project.KEY);
        if (project == null) return false;
        return RsProjectSettingsServiceUtil.getExternalLinterSettings(project).getRunOnTheFly();
    }

    @Override
    public void setSelected(AnActionEvent e, boolean state) {
        Project project = e.getData(consulo.project.Project.KEY);
        if (project == null) return;
        RsProjectSettingsServiceUtil.getExternalLinterSettings(project).modify(settings -> {
            settings.runOnTheFly = state;
        });
    }


    @Override
    public void update(AnActionEvent e) {
        super.update(e);
        Project project = e.getData(consulo.project.Project.KEY);
        String externalLinterName = project != null
            ? RsProjectSettingsServiceUtil.getExternalLinterSettings(project).getTool().getTitle()
            : "External Linter";
        e.getPresentation().setText(RsBundle.message("action.run.on.fly.text", externalLinterName));
    }
}
