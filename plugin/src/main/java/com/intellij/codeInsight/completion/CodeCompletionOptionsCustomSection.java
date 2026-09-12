package com.intellij.codeInsight.completion;
import consulo.configurable.UnnamedConfigurable;
import consulo.ui.Component;
/** Configurable contributed as an extra section of the code completion settings page. */
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
