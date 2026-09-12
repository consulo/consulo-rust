package com.intellij.refactoring.suggested;

import consulo.language.psi.PsiNamedElement;
import jakarta.annotation.Nonnull;

/** Data for a suggested rename refactoring: the declaration and its old name. */
public class SuggestedRenameData extends SuggestedRefactoringData {
    private final PsiNamedElement declaration;
    private final String oldName;

    public SuggestedRenameData(@Nonnull PsiNamedElement declaration, @Nonnull String oldName) {
        this.declaration = declaration;
        this.oldName = oldName;
    }

    @Nonnull public PsiNamedElement getDeclaration() { return declaration; }
    @Nonnull public String getOldName() { return oldName; }
}
