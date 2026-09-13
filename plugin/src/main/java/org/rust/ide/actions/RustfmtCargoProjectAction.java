/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.actions;

import org.rust.cargo.toolchain.RsToolchainLocator;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.action.DumbAwareAction;
import consulo.virtualFileSystem.util.VirtualFileUtil;
import consulo.virtualFileSystem.VirtualFile;
import consulo.ui.ex.action.LegacyDumbAwareAction;
import org.rust.cargo.api.model.CargoProject;
import org.rust.cargo.api.settings.RsProjectSettingsServiceUtil;
import org.rust.cargo.runconfig.command.CargoCommandConfiguration;
import org.rust.ide.rustfmt.Rustfmt;
import org.rust.cargo.toolchain.tools.Rustup;
import org.rust.openapiext.OpenApiUtil;
import org.rust.stdext.RsResult;
import consulo.annotation.component.ActionImpl;
import consulo.annotation.component.ActionParentRef;
import consulo.annotation.component.ActionRefAnchor;
import consulo.annotation.component.ActionRef;
import org.rust.cargo.runconfig.RunConfigUtil;
import consulo.rust.localize.RustLocalize;
import consulo.localize.LocalizeValue;
import consulo.rust.icon.RustIconGroup;

@ActionImpl(
    id = "Cargo.RustfmtCargoProject",
    parents = @ActionParentRef(
        value = @ActionRef(id = "CodeMenu"),
        anchor = ActionRefAnchor.LAST
    )
)
public class RustfmtCargoProjectAction extends LegacyDumbAwareAction {

    public RustfmtCargoProjectAction() {
        super(RustLocalize.actionCargoRustfmtcargoprojectText(), RustLocalize.actionCargoRustfmtcargoprojectDescription(), RustIconGroup.rust());
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
        org.rust.ide.rustfmt.RustfmtWatcher.saveAllDocumentsAsTheyAre(false);
        if (Rustup.checkNeedInstallRustfmt(ctx.cargoProject.getProject(), org.rust.cargo.project.model.CargoProjectLocator.getWorkingDirectory(ctx.cargoProject))) return;
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
        VirtualFileUtil.markDirtyAndRefresh(!OpenApiUtil.isUnitTestMode(), true, true, rootDir);
    }

    private Context getContext(AnActionEvent e) {
        CargoProject cargoProject = org.rust.cargo.runconfig.RunConfigUtil.getAppropriateCargoProject(e.getDataContext());
        if (cargoProject == null) return null;
        Rustfmt rustfmt = Rustfmt.rustfmt(RsToolchainLocator.getToolchain(cargoProject.getProject()));
        if (rustfmt == null) return null;
        return new Context(cargoProject, rustfmt);
    }

    private record Context(CargoProject cargoProject, Rustfmt rustfmt) {}
}
