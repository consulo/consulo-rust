package com.intellij.ui.breadcrumbs;

import consulo.language.Language;
import consulo.language.psi.PsiElement;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.Collections;
import java.util.List;

/** IntelliJ-compat stub: Breadcrumbs provider interface. Consulo has no equivalent in v3. */
public interface BreadcrumbsProvider {
    Language[] getLanguages();
    boolean acceptElement(@Nonnull PsiElement e);

    @Nonnull
    default String getElementInfo(@Nonnull PsiElement e) { return ""; }

    @Nullable
    default String getElementTooltip(@Nonnull PsiElement e) { return null; }

    @Nonnull
    default List<? extends Object> getElementActions(@Nonnull PsiElement e) { return Collections.emptyList(); }
}
