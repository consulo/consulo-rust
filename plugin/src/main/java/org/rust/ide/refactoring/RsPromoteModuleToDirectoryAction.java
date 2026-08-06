/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.refactoring;

import consulo.language.Language;
import consulo.dataContext.DataContext;
import consulo.codeEditor.Editor;
import consulo.project.Project;
import consulo.util.io.FileUtil;
import consulo.language.psi.PsiDirectory;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiFile;
import consulo.language.editor.refactoring.action.RefactoringActionHandler;
import consulo.language.editor.refactoring.action.BaseRefactoringAction;
import consulo.language.editor.refactoring.move.fileOrDirectory.MoveFilesOrDirectoriesUtil;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.rust.RsBundle;
import org.rust.cargo.project.workspace.CargoWorkspace.TargetKind;
import org.rust.lang.RsConstants;
import org.rust.lang.RsLanguage;
import org.rust.lang.core.crate.Crate;
import org.rust.lang.core.psi.RsFile;
import org.rust.openapiext.CommandWriteActionUtilsUtil;

public class RsPromoteModuleToDirectoryAction extends BaseRefactoringAction {

    @Override
    protected boolean isEnabledOnElements(@Nonnull PsiElement[] elements) {
        for (PsiElement element : elements) {
            if (!isPromotable(element)) return false;
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
        return isPromotable(file);
    }

    @Nonnull
    @Override
    protected RefactoringActionHandler getHandler(@Nonnull DataContext dataContext) {
        return Handler.INSTANCE;
    }

    @Override
    protected boolean isAvailableInEditorOnly() {
        return false;
    }

    @Override
    protected boolean isAvailableForLanguage(@Nonnull Language language) {
        return language.is(RsLanguage.INSTANCE);
    }

    public static void expandModule(@Nonnull RsFile file) {
        org.rust.openapiext.OpenApiUtil.checkWriteAccessAllowed();

        String dirName = FileUtil.getNameWithoutExtension(file.getName());
        PsiDirectory parentDir = file.getContainingDirectory();
        if (parentDir == null) {
            throw new IllegalStateException("Can't expand file: no parent directory for " + file + " at " + file.getVirtualFile().getPath());
        }
        PsiDirectory directory = parentDir.createSubdirectory(dirName);
        MoveFilesOrDirectoriesUtil.doMoveFile(file, directory);
        String name = file.isCrateRoot() ? RsConstants.MAIN_RS_FILE : RsConstants.MOD_RS_FILE;
        file.setName(name);
    }

    private static boolean isPromotable(@Nonnull PsiElement element) {
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
        public void invoke(@Nonnull Project project, @Nonnull Editor editor, @Nonnull PsiFile file, @Nullable DataContext dataContext) {
            invoke(project, new PsiElement[]{file}, dataContext);
        }

        @Override
        public void invoke(@Nonnull Project project, @Nonnull PsiElement[] elements, @Nullable DataContext dataContext) {
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
