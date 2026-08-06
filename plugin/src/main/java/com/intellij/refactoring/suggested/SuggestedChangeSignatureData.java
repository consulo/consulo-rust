package com.intellij.refactoring.suggested;

import consulo.language.psi.PsiElement;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

/** IntelliJ-compat stub: signature-change refactoring data. */
public class SuggestedChangeSignatureData extends SuggestedRefactoringData {
    private final String name;
    private final SuggestedRefactoringState state;

    protected SuggestedChangeSignatureData(@Nonnull SuggestedRefactoringState state, @Nonnull String name) {
        this.state = state;
        this.name = name;
    }

    @Nonnull
    public static SuggestedChangeSignatureData create(@Nonnull SuggestedRefactoringState state, @Nonnull String name) {
        return new SuggestedChangeSignatureData(state, name);
    }

    @Nonnull public String getNameOfStuffToUpdate() { return name; }
    @Nonnull public SuggestedRefactoringState getState() { return state; }
    @Nonnull public SuggestedRefactoringSupport.Signature getOldSignature() { return state.getOldSignature(); }
    @Nonnull public SuggestedRefactoringSupport.Signature getNewSignature() { return state.getNewSignature(); }
    @Nullable public PsiElement getDeclaration() { return state.getDeclaration(); }
    @Nullable public PsiElement restoredDeclarationCopy() { return state.restoredDeclarationCopy(); }
}
