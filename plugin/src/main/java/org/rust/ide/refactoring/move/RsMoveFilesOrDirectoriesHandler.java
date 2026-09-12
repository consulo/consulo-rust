/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.refactoring.move;

import consulo.language.Language;
import consulo.dataContext.DataContext;
import consulo.codeEditor.Editor;
import consulo.project.Project;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiFile;
import consulo.language.psi.PsiReference;
import consulo.language.psi.PsiManager;
import consulo.language.psi.PsiWhiteSpace;
import consulo.language.psi.PsiComment;
import consulo.language.psi.PsiNamedElement;
import consulo.language.psi.PsiNameIdentifierOwner;
import consulo.language.psi.PsiDirectory;
import consulo.language.psi.PsiErrorElement;
import consulo.language.psi.PsiElementVisitor;
import consulo.language.psi.PsiElementResolveResult;
import consulo.language.psi.ResolveResult;
import consulo.language.psi.PsiPolyVariantReference;
import consulo.language.psi.PsiReferenceBase;
import consulo.language.psi.PsiUtilCore;
import consulo.language.psi.StubBasedPsiElement;
import consulo.language.psi.SmartPsiElementPointer;
import consulo.language.psi.ElementManipulators;
import consulo.language.psi.ElementManipulator;
import consulo.language.psi.LiteralTextEscaper;
import consulo.language.psi.NavigatablePsiElement;
import consulo.language.psi.PsiLanguageInjectionHost;
import consulo.language.psi.ContributedReferenceHost;
import consulo.language.psi.SyntaxTraverser;
import consulo.language.psi.util.PsiTreeUtil;
import consulo.language.editor.refactoring.move.MoveCallback;
import consulo.language.editor.refactoring.move.fileOrDirectory.MoveFilesOrDirectoriesHandler;
import consulo.language.editor.refactoring.move.fileOrDirectory.MoveFilesOrDirectoriesUtil;
import consulo.language.editor.refactoring.util.CommonRefactoringUtil;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.rust.lang.RsLanguage;
import org.rust.lang.core.psi.RsFile;

import java.util.*;
import java.util.stream.Collectors;
import consulo.language.psi.PsiFile;
import consulo.annotation.component.ExtensionImpl;
import org.rust.lang.core.psi.ext.RsElementUtil;

@ExtensionImpl(id = "rust.moveFilesOrDirectories", order = "first, before moveJavaFileOrDir, before moveFileOrDir")
public class RsMoveFilesOrDirectoriesHandler extends MoveFilesOrDirectoriesHandler {

    public boolean supportsLanguage(@Nonnull Language language) {
        return language.is(RsLanguage.INSTANCE);
    }

    @Override
    public @Nullable PsiElement adjustTargetForMove(@Nullable DataContext dataContext, @Nullable PsiElement targetContainer) {
        if (targetContainer instanceof PsiFile) {
            return ((PsiFile) targetContainer).getContainingDirectory();
        }
        return targetContainer;
    }

    @Nullable
    @Override
    public PsiElement[] adjustForMove(
        @Nullable Project project,
        @Nonnull PsiElement [] elements,
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
        @Nonnull PsiElement [] elements,
        @Nullable PsiElement targetContainer
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

        return super.canMove(elements, adjustedTargetContainer);
    }

    @Override
    public void doMove(
        @Nonnull Project project,
        @Nonnull PsiElement [] elements,
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
        @Nonnull PsiElement element,
        @Nonnull Project project,
        @Nullable DataContext dataContext,
        @Nullable PsiReference reference,
        @Nullable Editor editor
    ) {
        if (!canMove(new PsiElement[]{element}, null)) return false;
        return super.tryToMove(element, project, dataContext, reference, editor);
    }

    @Nullable
    public static RsFile adjustForMove(@Nonnull PsiElement element) {
        if (element instanceof PsiDirectory) {
            return RsMoveDirectoryUtils.getOwningModAtDefaultLocation((PsiDirectory) element);
        }
        if (element instanceof RsFile) {
            return (RsFile) element;
        }
        return null;
    }

    private static boolean canBeMoved(@Nonnull RsFile file) {
        return file.getModName() != null
            && file.getCrateRoot() != null
            && file.getCrateRelativePath() != null
            && !file.isCrateRoot()
            && org.rust.lang.core.psi.ext.RsElementUtil.isAtLeastEdition2018(file)
            && file.getPathAttribute() == null;
    }
}
