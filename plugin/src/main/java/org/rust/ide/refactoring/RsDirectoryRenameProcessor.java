/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.refactoring;

import consulo.codeEditor.Editor;
import consulo.project.Project;
import consulo.language.psi.PsiDirectory;
import consulo.language.psi.PsiDirectoryContainer;
import consulo.language.psi.PsiElement;
import consulo.language.editor.refactoring.RefactoringSettings;
import consulo.language.editor.refactoring.rename.RenameDialog;
import consulo.language.editor.refactoring.rename.RenamePsiFileProcessorBase;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.rust.cargo.project.model.CargoProjectServiceUtil;
import org.rust.lang.RsConstants;
import org.rust.openapiext.Testmark;

import java.util.Map;
import consulo.annotation.component.ExtensionImpl;

@ExtensionImpl(id = "rsDirectoryRenameProcessor", order = "before rsRenameProcessor")
public class RsDirectoryRenameProcessor extends RenamePsiFileProcessorBase {

    @Nonnull
    @Override
    public RenameDialog createRenameDialog(@Nonnull Project project, @Nonnull PsiElement element, @Nullable PsiElement nameSuggestionContext, @Nullable Editor editor) {
        return super.createRenameDialog(project, toDir(element), nameSuggestionContext, editor);
    }

    @Override
    public boolean canProcessElement(@Nonnull PsiElement element) {
        if (!(element instanceof PsiDirectory || element instanceof PsiDirectoryContainer)) return false;
        return CargoProjectServiceUtil.getCargoProjects(element.getProject()).findProjectForFile(toDir(element).getVirtualFile()) != null;
    }

    @Override
    public void prepareRenaming(@Nonnull PsiElement element, @Nonnull String newName, @Nonnull Map<PsiElement, String> allRenames) {
        Testmarks.RustDirRenameHandler.hit();
        super.prepareRenaming(element, newName, allRenames);
        if (!RefactoringSettings.getInstance().RENAME_SEARCH_FOR_REFERENCES_FOR_DIRECTORY) return;

        PsiDirectory dir = toDir(element);
        PsiElement modrs = dir.findFile(RsConstants.MOD_RS_FILE);
        if (modrs == null) {
            PsiDirectory parentDir = dir.getParentDirectory();
            if (parentDir != null) {
                modrs = parentDir.findFile(dir.getName() + ".rs");
            }
        }
        if (modrs == null) return;
        allRenames.put(modrs, newName);
    }

    @Nonnull
    private static PsiDirectory toDir(@Nonnull PsiElement element) {
        if (element instanceof PsiDirectoryContainer) {
            return ((PsiDirectoryContainer) element).getDirectories()[0];
        }
        return (PsiDirectory) element;
    }

    public static class Testmarks {
        public static final Testmark RustDirRenameHandler = new Testmark() {};
    }
}
