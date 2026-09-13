package com.intellij.ide.wizard;

import consulo.project.Project;
import jakarta.annotation.Nonnull;

public interface NewProjectWizardStep {
    default void setupUI(@Nonnull com.intellij.ui.dsl.builder.Panel builder) {}
    default void setupProject(@Nonnull Project project) {}
}
