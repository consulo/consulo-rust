package com.intellij.refactoring.suggested;

import consulo.language.psi.PsiElement;
import jakarta.annotation.Nullable;

/** IntelliJ-compat stub: transient state for suggested refactoring. */
public interface SuggestedRefactoringState {
    default SuggestedRefactoringSupport.Signature getOldSignature() { return null; }
    default SuggestedRefactoringSupport.Signature getNewSignature() { return null; }
    @Nullable
    default PsiElement getDeclaration() { return null; }
    @Nullable
    default PsiElement restoredDeclarationCopy() { return null; }
}
