package com.intellij.refactoring.actions;

import consulo.language.Language;
import consulo.language.editor.refactoring.action.RefactoringActionHandler;
import consulo.language.editor.refactoring.RefactoringSupportProvider;
import consulo.ui.ex.action.AnAction;
import jakarta.annotation.Nonnull;

/** Base action for "Extract Super" refactorings: gated per language and delegating to a refactoring handler. */
public abstract class ExtractSuperActionBase extends AnAction {
    // AnAction.setInjectedContext is public, not protected — subclasses override via AnAction API

    protected boolean isAvailableForLanguage(@Nonnull Language language) { return false; }

    @Nonnull
    protected RefactoringActionHandler getRefactoringHandler(@Nonnull RefactoringSupportProvider provider) {
        throw new UnsupportedOperationException();
    }
}
