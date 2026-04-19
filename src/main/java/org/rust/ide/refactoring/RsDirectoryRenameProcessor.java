/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.refactoring;

import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiDirectory;
import com.intellij.psi.PsiDirectoryContainer;
import com.intellij.psi.PsiElement;
import com.intellij.refactoring.RefactoringSettings;
import com.intellij.refactoring.rename.RenameDialog;
import com.intellij.refactoring.rename.RenamePsiFileProcessor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.rust.cargo.project.model.CargoProjectServiceUtil;
import org.rust.lang.RsConstants;
import org.rust.openapiext.Testmark;

import java.util.Map;

public class RsDirectoryRenameProcessor extends RenamePsiFileProcessor {

    @NotNull
    @Override
    public RenameDialog createRenameDialog(@NotNull Project project, @NotNull PsiElement element, @Nullable PsiElement nameSuggestionContext, @Nullable Editor editor) {
        return super.createRenameDialog(project, toDir(element), nameSuggestionContext, editor);
    }

    @Override
    public boolean canProcessElement(@NotNull PsiElement element) {
        if (!(element instanceof PsiDirectory || element instanceof PsiDirectoryContainer)) return false;
        return CargoProjectServiceUtil.getCargoProjects(element.getProject()).findProjectForFile(toDir(element).getVirtualFile()) != null;
    }

    @Override
    public void prepareRenaming(@NotNull PsiElement element, @NotNull String newName, @NotNull Map<PsiElement, String> allRenames) {
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

    @NotNull
    private static PsiDirectory toDir(@NotNull PsiElement element) {
        if (element instanceof PsiDirectoryContainer) {
            return ((PsiDirectoryContainer) element).getDirectories()[0];
        }
        return (PsiDirectory) element;
    }

    public static class Testmarks {
        public static final Testmark RustDirRenameHandler = new Testmark() {};
    }
}
