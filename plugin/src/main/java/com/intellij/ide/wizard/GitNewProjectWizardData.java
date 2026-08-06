package com.intellij.ide.wizard;

import jakarta.annotation.Nullable;

/** IntelliJ-compat stub for git integration inside the new-project wizard. */
public interface GitNewProjectWizardData {
    boolean getGit();

    @Nullable
    static GitNewProjectWizardData getGitData(Object step) { return null; }
}
