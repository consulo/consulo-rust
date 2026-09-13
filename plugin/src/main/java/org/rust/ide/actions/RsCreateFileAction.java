/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.actions;

import consulo.application.dumb.DumbAware;
import consulo.dataContext.DataContext;
import consulo.ide.action.CreateFileFromTemplateAction;
import consulo.ide.action.CreateFileFromTemplateDialog;
import consulo.language.editor.CommonDataKeys;
import consulo.language.psi.PsiDirectory;
import consulo.localize.LocalizeValue;
import consulo.project.Project;
import consulo.virtualFileSystem.VirtualFile;
import consulo.virtualFileSystem.util.VirtualFileUtil;
import jakarta.annotation.Nonnull;
import org.rust.RsBundle;
import org.rust.cargo.project.model.CargoProjectServiceUtil;
import org.rust.icons.RsIcons;
import consulo.annotation.component.ActionImpl;
import consulo.annotation.component.ActionParentRef;
import consulo.annotation.component.ActionRefAnchor;
import consulo.annotation.component.ActionRef;

@ActionImpl(
    id = "Rust.NewRustFile",
    parents = @ActionParentRef(
        value = @ActionRef(id = "NewGroup"),
        anchor = ActionRefAnchor.BEFORE,
        relatedToAction = @ActionRef(id = "NewFile")
    )
)
public class RsCreateFileAction extends CreateFileFromTemplateAction implements DumbAware {

    private static final String CAPTION = RsBundle.message("rust.file");

    public RsCreateFileAction() {
        super(LocalizeValue.of(CAPTION), LocalizeValue.of(""), RsIcons.RUST_FILE);
    }

    @Override
    protected LocalizeValue getActionName(PsiDirectory directory, @Nonnull String newName, String templateName) {
        return LocalizeValue.of(CAPTION);
    }

    @Override
    protected boolean isAvailable(DataContext dataContext) {
        if (!super.isAvailable(dataContext)) return false;
        Project project = dataContext.getData(CommonDataKeys.PROJECT);
        if (project == null) return false;
        VirtualFile vFile = dataContext.getData(CommonDataKeys.VIRTUAL_FILE);
        if (vFile == null) return false;
        return CargoProjectServiceUtil.getCargoProjects(project).getAllProjects().stream().anyMatch(cargoProject -> {
            VirtualFile rootDir = cargoProject.getRootDir();
            if (rootDir == null) return false;
            return VirtualFileUtil.isAncestor(rootDir, vFile, false);
        });
    }

    @Override
    protected void buildDialog(@Nonnull Project project, @Nonnull PsiDirectory directory, @Nonnull CreateFileFromTemplateDialog.Builder builder) {
        builder.setTitle(LocalizeValue.of(CAPTION))
            .addKind(LocalizeValue.of(RsBundle.message("list.item.empty.file")), RsIcons.RUST_FILE, "Rust File");
    }
}
