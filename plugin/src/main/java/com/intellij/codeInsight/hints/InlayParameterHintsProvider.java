package com.intellij.codeInsight.hints;

import consulo.language.editor.inlay.Option;
import consulo.language.psi.PsiElement;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.Collections;
import java.util.List;
import java.util.Set;

/** Supplies parameter-name hints shown as inlays next to call arguments. */
public interface InlayParameterHintsProvider {
    @Nonnull List<InlayInfo> getParameterHints(@Nonnull PsiElement element);
    @Nullable default HintInfo getHintInfo(@Nonnull PsiElement element) { return null; }
    @Nonnull default Set<String> getDefaultBlackList() { return Collections.emptySet(); }
    @Nonnull default List<Option> getSupportedOptions() { return Collections.emptyList(); }
    @Nonnull default String getInlayPresentation(@Nonnull String inlayText) { return inlayText; }
}
