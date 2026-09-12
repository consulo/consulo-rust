package com.intellij.codeInsight.hints;

import jakarta.annotation.Nonnull;

import javax.swing.JComponent;
import javax.swing.event.ChangeListener;
import java.util.Collections;
import java.util.List;

public interface ImmediateConfigurable {
    @Nonnull default JComponent createComponent(@Nonnull ChangeListener listener) { throw new UnsupportedOperationException(); }
    @Nonnull default String getMainCheckboxText() { return ""; }
    @Nonnull default List<Case> getCases() { return Collections.emptyList(); }

    class Case {
        public Case(String name, String id, boolean enabled) {}
        public Case(String name, String id, boolean enabled, String description) {}
    }
}
