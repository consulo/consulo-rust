package com.intellij.ide.wizard;
import jakarta.annotation.Nonnull;
/** IntelliJ-compat stub for the language-specific entry in the new-project wizard. */
public interface LanguageNewProjectWizard {
    @Nonnull String getName();
    default int getOrdinal() { return 0; }
    @Nonnull NewProjectWizardStep createStep(@Nonnull NewProjectWizardLanguageStep parent);
}
