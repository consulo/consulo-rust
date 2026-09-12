package com.intellij.codeInsight.hints;

import consulo.codeEditor.Editor;
import consulo.language.editor.inlay.InlayGroup;
import consulo.language.psi.PsiFile;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import consulo.language.Language;

/** Provides inlay hints for a language, configured by settings of type {@code T}. */
public interface InlayHintsProvider<T> {
    @Nonnull default SettingsKey<T> getKey() { throw new UnsupportedOperationException(); }
    @Nonnull default String getName() { return ""; }
    @Nullable default String getPreviewText() { return null; }
    @Nonnull default InlayGroup getGroup() { return InlayGroup.OTHER_GROUP; }
    @Nonnull default ImmediateConfigurable createConfigurable(@Nonnull T settings) { throw new UnsupportedOperationException(); }
    @Nonnull default T createSettings() { throw new UnsupportedOperationException(); }
    @Nullable default InlayHintsCollector getCollectorFor(@Nonnull PsiFile file, @Nonnull Editor editor, @Nonnull T settings, @Nonnull InlayHintsSink sink) { return null; }
    default boolean isVisibleInSettings() { return true; }
    default boolean isLanguageSupported(@Nonnull consulo.language.Language language) { return true; }

    interface ChangeListener {
        void settingsChanged();
    }
}
