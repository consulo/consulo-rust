package com.intellij.refactoring.suggested;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

/** IntelliJ-compat stub: part of the suggested refactoring framework. */
public abstract class SuggestedRefactoringAvailability {
    protected final SuggestedRefactoringSupport refactoringSupport;

    public SuggestedRefactoringAvailability(@Nonnull SuggestedRefactoringSupport refactoringSupport) {
        this.refactoringSupport = refactoringSupport;
    }

    @Nullable
    public SuggestedRefactoringData detectAvailableRefactoring(@Nonnull SuggestedRefactoringState state) { return null; }

    public boolean shouldSuppressRefactoringForDeclaration(@Nonnull SuggestedRefactoringState state) { return false; }

    protected boolean hasParameterAddedRemovedOrReordered(SuggestedRefactoringSupport.Signature oldSignature,
                                                           SuggestedRefactoringSupport.Signature newSignature) {
        if (oldSignature.getParameters().size() != newSignature.getParameters().size()) return true;
        for (int i = 0; i < oldSignature.getParameters().size(); i++) {
            if (oldSignature.getParameters().get(i).getId() != newSignature.getParameters().get(i).getId()) return true;
        }
        return false;
    }

    protected boolean hasTypeChanges(SuggestedRefactoringSupport.Signature oldSignature,
                                     SuggestedRefactoringSupport.Signature newSignature) {
        return false;
    }
}
