/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.cargo.toolchain.actions;

import org.rust.cargo.toolchain.RsToolchainLocator;
import consulo.project.ui.notification.Notification;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.action.DumbAwareAction;
import consulo.project.Project;
import org.rust.RsBundle;
import org.rust.cargo.api.settings.RsProjectSettingsServiceUtil;
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
        Project project = e.getData(consulo.project.Project.KEY);
        if (project == null) return;
        RsToolchainBase toolchain = RsToolchainLocator.getToolchain(project);
        if (toolchain == null) return;
        Cargo cargo = Cargo.cargo(toolchain);
        if (cargo == null) return;
        Notification.get(e).expire();
        cargo.installBinaryCrate(project, crateName);
    }
}
