/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.refactoring;

import com.intellij.lang.Language;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.psi.*;
import com.intellij.psi.impl.file.PsiFileImplUtil;
import com.intellij.refactoring.RefactoringActionHandler;
import com.intellij.refactoring.actions.BaseRefactoringAction;
import com.intellij.refactoring.move.moveFilesOrDirectories.MoveFilesOrDirectoriesUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.rust.RsBundle;
import org.rust.lang.RsConstants;
import org.rust.lang.RsLanguage;
import org.rust.lang.core.psi.RsFile;
import org.rust.openapiext.OpenApiUtil;
import com.intellij.psi.PsiFile;

public class RsDowngradeModuleToFile extends BaseRefactoringAction {

    @Override
    protected boolean isEnabledOnElements(@NotNull PsiElement[] elements) {
        for (PsiElement element : elements) {
            if (!isDirectoryMod(element)) return false;
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
        return isDirectoryMod(file);
    }

    @NotNull
    @Override
    protected RefactoringActionHandler getHandler(@NotNull DataContext dataContext) {
        return HANDLER;
    }

    @Override
    protected boolean isAvailableInEditorOnly() {
        return false;
    }

    @Override
    protected boolean isAvailableForLanguage(@NotNull Language language) {
        return language.is(RsLanguage.INSTANCE) || language.is(Language.ANY);
    }

    private static final RefactoringActionHandler HANDLER = new RefactoringActionHandler() {
        @Override
        public void invoke(@NotNull Project project, @NotNull Editor editor, @NotNull PsiFile file, @Nullable DataContext dataContext) {
            invoke(project, new PsiElement[]{file}, dataContext);
        }

        @Override
        public void invoke(@NotNull Project project, @NotNull PsiElement[] elements, @Nullable DataContext dataContext) {
            OpenApiUtil.runWriteCommandAction(project, RsBundle.message("action.Rust.RsDowngradeModuleToFile.text"), () -> {
                for (PsiElement element : elements) {
                    contractModule((PsiFileSystemItem) element);
                }
            });
        }
    };

    private static void contractModule(@NotNull PsiFileSystemItem fileOrDirectory) {
        OpenApiUtil.checkWriteAccessAllowed();

        RsFile file;
        PsiDirectory dir;
        if (fileOrDirectory instanceof RsFile) {
            file = (RsFile) fileOrDirectory;
            dir = file.getParent();
        } else if (fileOrDirectory instanceof PsiDirectory) {
            PsiElement[] children = ((PsiDirectory) fileOrDirectory).getChildren();
            if (children.length != 1) {
                throw new IllegalStateException("Can contract only files and directories");
            }
            file = (RsFile) children[0];
            dir = (PsiDirectory) fileOrDirectory;
        } else {
            throw new IllegalStateException("Can contract only files and directories");
        }

        PsiDirectory dst = dir.getParent();
        String fileName = dir.getName() + ".rs";
        PsiFileImplUtil.setName(file, fileName);
        MoveFilesOrDirectoriesUtil.doMoveFile(file, dst);
        dir.delete();
    }

    private static boolean isDirectoryMod(@NotNull PsiElement element) {
        if (element instanceof RsFile) {
            RsFile rsFile = (RsFile) element;
            return RsConstants.MOD_RS_FILE.equals(rsFile.getName())
                && rsFile.getContainingDirectory() != null
                && rsFile.getContainingDirectory().getChildren().length == 1;
        }
        if (element instanceof PsiDirectory) {
            PsiElement[] children = ((PsiDirectory) element).getChildren();
            if (children.length != 1) return false;
            PsiElement child = children[0];
            return child instanceof RsFile && RsConstants.MOD_RS_FILE.equals(((RsFile) child).getName());
        }
        return false;
    }
}
