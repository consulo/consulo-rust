package com.intellij.refactoring.suggested;
import jakarta.annotation.Nonnull;
/** Tracks declaration signature changes that drive a suggested refactoring. */
public abstract class SuggestedRefactoringStateChanges {
    protected final SuggestedRefactoringSupport refactoringSupport;
    public SuggestedRefactoringStateChanges(@Nonnull SuggestedRefactoringSupport support) {
        this.refactoringSupport = support;
    }
    public SuggestedRefactoringSupport.Signature signature(Object anchor, SuggestedRefactoringState prevState) { return null; }
    public SuggestedRefactoringState createInitialState(Object anchor) { return null; }
    public SuggestedRefactoringState updateState(SuggestedRefactoringState state, Object anchor) { return state; }
    protected SuggestedRefactoringSupport.Signature matchParametersWithPrevState(SuggestedRefactoringSupport.Signature signature, Object anchor, SuggestedRefactoringState prevState) { return signature; }
}
