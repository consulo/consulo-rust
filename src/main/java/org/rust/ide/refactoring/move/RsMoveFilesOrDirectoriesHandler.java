/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.refactoring.move;

import com.intellij.lang.Language;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.psi.*;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.refactoring.move.MoveCallback;
import com.intellij.refactoring.move.moveFilesOrDirectories.MoveFilesOrDirectoriesHandler;
import com.intellij.refactoring.move.moveFilesOrDirectories.MoveFilesOrDirectoriesUtil;
import com.intellij.refactoring.util.CommonRefactoringUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.rust.lang.RsLanguage;
import org.rust.lang.core.psi.RsFile;

import java.util.*;
import java.util.stream.Collectors;
import com.intellij.psi.PsiFile;

public class RsMoveFilesOrDirectoriesHandler extends MoveFilesOrDirectoriesHandler {

    @Override
    public boolean supportsLanguage(@NotNull Language language) {
        return language.is(RsLanguage.INSTANCE);
    }

    @Override
    public @Nullable PsiElement adjustTargetForMove(@Nullable DataContext dataContext, @Nullable PsiElement targetContainer) {
        if (targetContainer instanceof PsiFile) {
            return ((PsiFile) targetContainer).getContainingDirectory();
        }
        return targetContainer;
    }

    @Override
    public PsiElement @Nullable [] adjustForMove(
        @Nullable Project project,
        @NotNull PsiElement @NotNull [] elements,
        @Nullable PsiElement targetElement
    ) {
        Set<PsiElement> elementsWithRelated = new LinkedHashSet<>();
        for (PsiElement element : elements) {
            RsFile file = adjustForMove(element);
            PsiDirectory directory = file != null ? file.getOwnedDirectory() : null;
            if (file != null && directory != null) {
                elementsWithRelated.add(file);
                elementsWithRelated.add(directory);
            } else {
                elementsWithRelated.add(element);
            }
        }
        PsiElement[] adjusted = super.adjustForMove(project, elementsWithRelated.toArray(PsiElement.EMPTY_ARRAY), targetElement);
        if (adjusted == null) return null;
        return Arrays.stream(adjusted).filter(Objects::nonNull).toArray(PsiElement[]::new);
    }

    @Override
    public boolean canMove(
        @NotNull PsiElement @NotNull [] elements,
        @Nullable PsiElement targetContainer,
        @Nullable PsiReference reference
    ) {
        PsiElement[] ancestors = PsiTreeUtil.filterAncestors(elements);
        List<RsFile> files = new ArrayList<>();
        for (PsiElement element : ancestors) {
            RsFile file = adjustForMove(element);
            if (file == null) return false;
            files.add(file);
        }
        if (files.isEmpty()) return false;
        if (!files.stream().allMatch(RsMoveFilesOrDirectoriesHandler::canBeMoved)) return false;

        Set<PsiElement> superMods = files.stream()
            .map(f -> (PsiElement) f.getParent())
            .collect(Collectors.toSet());
        if (superMods.size() != 1) return false;

        PsiElement adjustedTargetContainer = adjustTargetForMove(null, targetContainer);
        if (files.stream().anyMatch(f -> f.getParent() == adjustedTargetContainer)) {
            return false;
        }

        return super.canMove(elements, adjustedTargetContainer, reference);
    }

    @Override
    public void doMove(
        @NotNull Project project,
        @NotNull PsiElement @NotNull [] elements,
        @Nullable PsiElement targetContainer,
        @Nullable MoveCallback moveCallback
    ) {
        if (!CommonRefactoringUtil.checkReadOnlyStatusRecursively(project, Arrays.asList(elements), true)) return;

        PsiElement adjustedTargetContainer = adjustTargetForMove(null, targetContainer);
        PsiElement[] adjustedElements = adjustForMove(project, elements, adjustedTargetContainer);
        if (adjustedElements == null) return;

        PsiDirectory targetDirectory = MoveFilesOrDirectoriesUtil.resolveToDirectory(project, adjustedTargetContainer);
        if (adjustedTargetContainer != null && targetDirectory == null) return;
        PsiDirectory initialTargetDirectory = MoveFilesOrDirectoriesUtil.getInitialTargetDirectory(targetDirectory, elements);

        new RsMoveFilesOrDirectoriesDialog(project, adjustedElements, initialTargetDirectory, moveCallback).show();
    }

    @Override
    public boolean tryToMove(
        @NotNull PsiElement element,
        @NotNull Project project,
        @Nullable DataContext dataContext,
        @Nullable PsiReference reference,
        @Nullable Editor editor
    ) {
        if (!canMove(new PsiElement[]{element}, null, reference)) return false;
        return super.tryToMove(element, project, dataContext, reference, editor);
    }

    @Nullable
    public static RsFile adjustForMove(@NotNull PsiElement element) {
        if (element instanceof PsiDirectory) {
            return RsMoveDirectoryUtils.getOwningModAtDefaultLocation((PsiDirectory) element);
        }
        if (element instanceof RsFile) {
            return (RsFile) element;
        }
        return null;
    }

    private static boolean canBeMoved(@NotNull RsFile file) {
        return file.getModName() != null
            && file.getCrateRoot() != null
            && file.getCrateRelativePath() != null
            && !file.isCrateRoot()
            && org.rust.lang.core.psi.ext.RsElementUtil.isAtLeastEdition2018(file)
            && file.getPathAttribute() == null;
    }
}
