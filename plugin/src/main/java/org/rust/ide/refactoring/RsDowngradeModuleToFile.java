/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.refactoring;

import consulo.language.Language;
import consulo.dataContext.DataContext;
import consulo.codeEditor.Editor;
import consulo.language.psi.*;
import consulo.project.Project;
import consulo.language.editor.refactoring.action.RefactoringActionHandler;
import consulo.language.editor.refactoring.action.BaseRefactoringAction;
import consulo.language.editor.refactoring.move.fileOrDirectory.MoveFilesOrDirectoriesUtil;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.rust.RsBundle;
import org.rust.lang.RsConstants;
import org.rust.lang.RsLanguage;
import org.rust.lang.core.psi.impl.RsFile;
import org.rust.openapiext.OpenApiUtil;
import consulo.language.psi.PsiFile;
import consulo.annotation.component.ActionImpl;
import consulo.annotation.component.ActionParentRef;
import consulo.annotation.component.ActionRefAnchor;
import consulo.annotation.component.ActionRef;

@ActionImpl(
    id = "Rust.RsDowngradeModuleToFile",
    parents = @ActionParentRef(
        value = @ActionRef(id = "RefactoringMenu")
    )
)
public class RsDowngradeModuleToFile extends BaseRefactoringAction {

    @Override
    protected boolean isEnabledOnElements(@Nonnull PsiElement[] elements) {
        for (PsiElement element : elements) {
            if (!isDirectoryMod(element)) return false;
        }
        return true;
    }

    @Override
    protected boolean isAvailableOnElementInEditorAndFile(
        @Nonnull PsiElement element,
        @Nonnull Editor editor,
        @Nonnull PsiFile file,
        @Nonnull DataContext context
    ) {
        return isDirectoryMod(file);
    }

    @Nonnull
    @Override
    protected RefactoringActionHandler getHandler(@Nonnull DataContext dataContext) {
        return HANDLER;
    }

    @Override
    protected boolean isAvailableInEditorOnly() {
        return false;
    }

    @Override
    protected boolean isAvailableForLanguage(@Nonnull Language language) {
        return language.is(RsLanguage.INSTANCE) || language.is(Language.ANY);
    }

    private static final RefactoringActionHandler HANDLER = new RefactoringActionHandler() {
        @Override
        public void invoke(@Nonnull Project project, @Nonnull Editor editor, @Nonnull PsiFile file, @Nullable DataContext dataContext) {
            invoke(project, new PsiElement[]{file}, dataContext);
        }

        @Override
        public void invoke(@Nonnull Project project, @Nonnull PsiElement[] elements, @Nullable DataContext dataContext) {
            OpenApiUtil.runWriteCommandAction(project, RsBundle.message("action.Rust.RsDowngradeModuleToFile.text"), () -> {
                for (PsiElement element : elements) {
                    contractModule((PsiFileSystemItem) element);
                }
            });
        }
    };

    private static void contractModule(@Nonnull PsiFileSystemItem fileOrDirectory) {
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
        file.setName(fileName);
        MoveFilesOrDirectoriesUtil.doMoveFile(file, dst);
        dir.delete();
    }

    private static boolean isDirectoryMod(@Nonnull PsiElement element) {
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
