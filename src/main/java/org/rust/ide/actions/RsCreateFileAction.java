/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.actions;

import com.intellij.ide.actions.CreateFileFromTemplateAction;
import com.intellij.ide.actions.CreateFileFromTemplateDialog;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiDirectory;
import org.jetbrains.annotations.NotNull;
import org.rust.RsBundle;
import org.rust.cargo.project.model.CargoProjectServiceUtil;
import org.rust.ide.icons.RsIcons;

public class RsCreateFileAction extends CreateFileFromTemplateAction implements DumbAware {

    private static final String CAPTION = RsBundle.message("rust.file");

    public RsCreateFileAction() {
        super(CAPTION, "", RsIcons.RUST_FILE);
    }

    @Override
    protected String getActionName(PsiDirectory directory, @NotNull String newName, String templateName) {
        return CAPTION;
    }

    @Override
    protected boolean isAvailable(DataContext dataContext) {
        if (!super.isAvailable(dataContext)) return false;
        Project project = CommonDataKeys.PROJECT.getData(dataContext);
        if (project == null) return false;
        VirtualFile vFile = CommonDataKeys.VIRTUAL_FILE.getData(dataContext);
        if (vFile == null) return false;
        return CargoProjectServiceUtil.getCargoProjects(project).getAllProjects().stream().anyMatch(cargoProject -> {
            VirtualFile rootDir = cargoProject.getRootDir();
            if (rootDir == null) return false;
            return VfsUtil.isAncestor(rootDir, vFile, false);
        });
    }

    @Override
    protected void buildDialog(@NotNull Project project, @NotNull PsiDirectory directory, CreateFileFromTemplateDialog.@NotNull Builder builder) {
        builder.setTitle(CAPTION)
            .addKind(RsBundle.message("list.item.empty.file"), RsIcons.RUST_FILE, "Rust File");
    }
}
