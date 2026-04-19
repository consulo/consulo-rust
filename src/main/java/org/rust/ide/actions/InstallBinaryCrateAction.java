/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.actions;

import com.intellij.notification.Notification;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.DumbAwareAction;
import com.intellij.openapi.project.Project;
import org.rust.RsBundle;
import org.rust.cargo.project.settings.RsProjectSettingsServiceUtil;
import org.rust.cargo.toolchain.RsToolchainBase;
import org.rust.cargo.toolchain.tools.Cargo;

public class InstallBinaryCrateAction extends DumbAwareAction {
    private final String crateName;

    public InstallBinaryCrateAction(String crateName) {
        super(RsBundle.message("action.install.text"));
        this.crateName = crateName;
    }

    @Override
    public void actionPerformed(AnActionEvent e) {
        Project project = e.getProject();
        if (project == null) return;
        RsToolchainBase toolchain = RsProjectSettingsServiceUtil.getToolchain(project);
        if (toolchain == null) return;
        Cargo cargo = Cargo.cargo(toolchain);
        if (cargo == null) return;
        Notification.get(e).expire();
        cargo.installBinaryCrate(project, crateName);
    }
}
