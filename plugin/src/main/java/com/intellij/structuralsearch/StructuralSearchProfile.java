package com.intellij.structuralsearch;

import com.intellij.structuralsearch.impl.matcher.PatternTreeContext;
import consulo.language.Language;
import consulo.language.file.LanguageFileType;
import consulo.language.psi.PsiElement;
import consulo.project.Project;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

/** IntelliJ-compat stub. Structural search is not in Consulo. */
public abstract class StructuralSearchProfile {
    public PsiElement[] createPatternTree(@Nonnull String text,
                                           @Nonnull PatternTreeContext context,
                                           @Nonnull LanguageFileType fileType,
                                           @Nonnull Language language,
                                           @Nullable String contextId,
                                           @Nonnull Project project,
                                           boolean physical) {
        return PsiElement.EMPTY_ARRAY;
    }
}
