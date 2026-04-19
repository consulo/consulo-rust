/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.actions;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import org.rust.cargo.CargoConstants;
import org.rust.cargo.project.model.CargoProjectServiceUtil;
import org.rust.cargo.project.settings.RsProjectSettingsServiceUtil;
import org.rust.cargo.runconfig.command.RunCargoCommandActionBase;
import org.rust.cargo.toolchain.RsToolchainBase;
import org.rust.cargo.toolchain.tools.Cargo;
import org.rust.ide.actions.ui.CargoNewCrateDialog;
import org.rust.ide.actions.ui.CargoNewCrateSettings;
import org.rust.ide.actions.ui.CargoNewCrateUI;
import org.rust.openapiext.OpenApiUtil;
import org.rust.stdext.RsResult;

import java.nio.file.Path;

public class RsCreateCrateAction extends RunCargoCommandActionBase {
    @Override
    public void actionPerformed(AnActionEvent e) {
        DataContext dataContext = e.getDataContext();
        Project project = CommonDataKeys.PROJECT.getData(dataContext);
        if (project == null) return;
        VirtualFile root = getRootFolder(dataContext);
        if (root == null) return;
        RsToolchainBase toolchain = RsProjectSettingsServiceUtil.getToolchain(project);
        if (toolchain == null) return;

        CargoNewCrateUI ui = CargoNewCrateDialog.showCargoNewCrateUI(project, root);
        CargoNewCrateSettings settings = ui.selectCargoCrateSettings();
        if (settings != null) {
            createProject(project, toolchain, root, settings.getCrateName(), settings.getBinary());
        }
    }

    private VirtualFile getRootFolder(DataContext dataContext) {
        VirtualFile file = CommonDataKeys.VIRTUAL_FILE.getData(dataContext);
        if (file == null) return null;
        return file.isDirectory() ? file : file.getParent();
    }

    private void createProject(
        Project project,
        RsToolchainBase toolchain,
        VirtualFile root,
        String name,
        boolean binary
    ) {
        org.rust.cargo.project.model.CargoProject cargoProject = CargoProjectServiceUtil.getCargoProjects(project).findProjectForFile(root);
        Path workspaceRoot = cargoProject != null && cargoProject.getWorkspaceRootDir() != null
            ? OpenApiUtil.getPathAsPath(cargoProject.getWorkspaceRootDir())
            : null;
        Cargo cargo = Cargo.cargoOrWrapper(toolchain, workspaceRoot);

        VirtualFile targetDir = ApplicationManager.getApplication().runWriteAction(
            (com.intellij.openapi.util.Computable<VirtualFile>) () -> {
                try {
                    return root.createChildDirectory(this, name);
                } catch (java.io.IOException ex) {
                    throw new RuntimeException(ex);
                }
            }
        );
        RsResult.unwrapOrThrow(cargo.init(project, project, targetDir, name, binary, "none"));

        VirtualFile manifest = targetDir.findChild(CargoConstants.MANIFEST_FILE);
        if (manifest != null) {
            CargoProjectServiceUtil.getCargoProjects(project).attachCargoProject(OpenApiUtil.getPathAsPath(manifest));
        }
    }
}
