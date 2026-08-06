package com.intellij.codeInsight.hints;

import consulo.language.Language;
import consulo.project.Project;
import consulo.ui.ex.action.AnAction;
import consulo.ui.ex.action.AnActionEvent;
import jakarta.annotation.Nonnull;

/** IntelliJ-compat stub — action that disables an inlay hints provider. */
public class InlayProviderDisablingAction extends AnAction {
    public InlayProviderDisablingAction(String name, Language language, Project project, SettingsKey<?> key) {}
    @Override public void actionPerformed(@Nonnull AnActionEvent e) {}
}
