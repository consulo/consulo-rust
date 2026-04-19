/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.actions;

import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.DumbAwareAction;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VirtualFile;
import org.rust.cargo.project.model.CargoProject;
import org.rust.cargo.project.settings.RsProjectSettingsServiceUtil;
import org.rust.cargo.runconfig.command.CargoCommandConfiguration;
import org.rust.cargo.toolchain.tools.Rustfmt;
import org.rust.cargo.toolchain.tools.Rustup;
import org.rust.openapiext.OpenApiUtil;
import org.rust.stdext.RsResult;

public class RustfmtCargoProjectAction extends DumbAwareAction {

    @Override
    public ActionUpdateThread getActionUpdateThread() {
        return ActionUpdateThread.BGT;
    }

    @Override
    public void update(AnActionEvent e) {
        super.update(e);
        e.getPresentation().setEnabled(getContext(e) != null);
    }

    @Override
    public void actionPerformed(AnActionEvent e) {
        Context ctx = getContext(e);
        if (ctx == null) return;
        OpenApiUtil.saveAllDocumentsAsTheyAre(false);
        if (Rustup.checkNeedInstallRustfmt(ctx.cargoProject.getProject(), CargoCommandConfiguration.getWorkingDirectory(ctx.cargoProject))) return;
        try {
            ctx.rustfmt.reformatCargoProject(ctx.cargoProject).unwrapOrElse(err -> {
                if (OpenApiUtil.isUnitTestMode()) throw new RuntimeException(err);
                return null;
            });
        } catch (Exception ex) {
            if (OpenApiUtil.isUnitTestMode()) throw ex;
            return;
        }
        VirtualFile rootDir = ctx.cargoProject.getRootDir();
        if (rootDir == null) return;
        VfsUtil.markDirtyAndRefresh(!OpenApiUtil.isUnitTestMode(), true, true, rootDir);
    }

    private Context getContext(AnActionEvent e) {
        CargoProject cargoProject = org.rust.cargo.runconfig.RunConfigUtil.getAppropriateCargoProject(e.getDataContext());
        if (cargoProject == null) return null;
        Rustfmt rustfmt = Rustfmt.rustfmt(RsProjectSettingsServiceUtil.getToolchain(cargoProject.getProject()));
        if (rustfmt == null) return null;
        return new Context(cargoProject, rustfmt);
    }

    private record Context(CargoProject cargoProject, Rustfmt rustfmt) {}
}
