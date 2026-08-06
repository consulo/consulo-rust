package com.intellij.codeInsight.completion;
import consulo.configurable.UnnamedConfigurable;
/** IntelliJ-compat stub. */
public interface CodeCompletionOptionsCustomSection extends UnnamedConfigurable {
    @Override
    default consulo.ui.Component createUIComponent() { return null; }
    @Override
    default boolean isModified() { return false; }
    @Override
    default void apply() {}
    @Override
    default void reset() {}
}
