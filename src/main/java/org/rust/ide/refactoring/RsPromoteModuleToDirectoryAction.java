/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.refactoring;

import com.intellij.lang.Language;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.psi.PsiDirectory;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.impl.file.PsiFileImplUtil;
import com.intellij.refactoring.RefactoringActionHandler;
import com.intellij.refactoring.actions.BaseRefactoringAction;
import com.intellij.refactoring.move.moveFilesOrDirectories.MoveFilesOrDirectoriesUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.rust.RsBundle;
import org.rust.cargo.project.workspace.CargoWorkspace.TargetKind;
import org.rust.lang.RsConstants;
import org.rust.lang.RsLanguage;
import org.rust.lang.core.crate.Crate;
import org.rust.lang.core.psi.RsFile;
import org.rust.openapiext.CommandWriteActionUtilsUtil;

public class RsPromoteModuleToDirectoryAction extends BaseRefactoringAction {

    @Override
    protected boolean isEnabledOnElements(@NotNull PsiElement[] elements) {
        for (PsiElement element : elements) {
            if (!isPromotable(element)) return false;
        }
        return true;
    }

    @Override
    protected boolean isAvailableOnElementInEditorAndFile(
        @NotNull PsiElement element,
        @NotNull Editor editor,
        @NotNull PsiFile file,
        @NotNull DataContext context
    ) {
        return isPromotable(file);
    }

    @NotNull
    @Override
    protected RefactoringActionHandler getHandler(@NotNull DataContext dataContext) {
        return Handler.INSTANCE;
    }

    @Override
    protected boolean isAvailableInEditorOnly() {
        return false;
    }

    @Override
    protected boolean isAvailableForLanguage(@NotNull Language language) {
        return language.is(RsLanguage.INSTANCE);
    }

    public static void expandModule(@NotNull RsFile file) {
        org.rust.openapiext.OpenApiUtil.checkWriteAccessAllowed();

        String dirName = FileUtil.getNameWithoutExtension(file.getName());
        PsiDirectory parentDir = file.getContainingDirectory();
        if (parentDir == null) {
            throw new IllegalStateException("Can't expand file: no parent directory for " + file + " at " + file.getVirtualFile().getPath());
        }
        PsiDirectory directory = parentDir.createSubdirectory(dirName);
        MoveFilesOrDirectoriesUtil.doMoveFile(file, directory);
        String name = file.isCrateRoot() ? RsConstants.MAIN_RS_FILE : RsConstants.MOD_RS_FILE;
        PsiFileImplUtil.setName(file, name);
    }

    private static boolean isPromotable(@NotNull PsiElement element) {
        if (!(element instanceof RsFile)) return false;
        RsFile rsFile = (RsFile) element;
        if (rsFile.isCrateRoot()) {
            if (rsFile.getName().equals(RsConstants.MAIN_RS_FILE)) return false;
            Crate crate = Crate.asNotFake(rsFile.getContainingCrate());
            if (crate == null) return false;
            TargetKind kind = crate.getKind();
            return isPromotableKind(kind);
        } else {
            return !rsFile.getName().equals(RsConstants.MOD_RS_FILE);
        }
    }

    private static boolean isPromotableKind(@Nullable TargetKind kind) {
        if (kind == null) return false;
        return kind instanceof TargetKind.Bin
            || kind instanceof TargetKind.Test
            || kind instanceof TargetKind.ExampleBin
            || kind instanceof TargetKind.Bench;
    }

    private static class Handler implements RefactoringActionHandler {
        static final Handler INSTANCE = new Handler();

        @Override
        public void invoke(@NotNull Project project, @NotNull Editor editor, @NotNull PsiFile file, @Nullable DataContext dataContext) {
            invoke(project, new PsiElement[]{file}, dataContext);
        }

        @Override
        public void invoke(@NotNull Project project, @NotNull PsiElement[] elements, @Nullable DataContext dataContext) {
            java.util.List<RsFile> files = new java.util.ArrayList<>();
            for (PsiElement element : elements) {
                if (element instanceof RsFile) {
                    files.add((RsFile) element);
                }
            }
            org.rust.openapiext.OpenApiUtil.runWriteCommandAction(
                project,
                RsBundle.message("action.Rust.RsPromoteModuleToDirectoryAction.text"),
                () -> {
                    for (RsFile file : files) {
                        expandModule(file);
                    }
                }
            );
        }
    }
}
