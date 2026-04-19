/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.actions;

import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.ToggleAction;
import com.intellij.openapi.project.Project;
import org.rust.RsBundle;
import org.rust.cargo.project.settings.RsExternalLinterProjectSettingsService;
import org.rust.cargo.project.settings.RsProjectSettingsServiceUtil;

public class ToggleExternalLinterOnTheFlyAction extends ToggleAction {

    @Override
    public boolean isSelected(AnActionEvent e) {
        Project project = e.getProject();
        if (project == null) return false;
        return RsProjectSettingsServiceUtil.getExternalLinterSettings(project).getRunOnTheFly();
    }

    @Override
    public void setSelected(AnActionEvent e, boolean state) {
        Project project = e.getProject();
        if (project == null) return;
        RsProjectSettingsServiceUtil.getExternalLinterSettings(project).modify(settings -> {
            settings.runOnTheFly = state;
        });
    }

    @Override
    public ActionUpdateThread getActionUpdateThread() {
        return ActionUpdateThread.BGT;
    }

    @Override
    public void update(AnActionEvent e) {
        super.update(e);
        Project project = e.getProject();
        String externalLinterName = project != null
            ? RsProjectSettingsServiceUtil.getExternalLinterSettings(project).getTool().getTitle()
            : "External Linter";
        e.getPresentation().setText(RsBundle.message("action.run.on.fly.text", externalLinterName));
    }
}
