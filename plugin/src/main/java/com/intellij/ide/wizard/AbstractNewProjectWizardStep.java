package com.intellij.ide.wizard;

/** IntelliJ-compat stub for the Kotlin new-project wizard step. */
public abstract class AbstractNewProjectWizardStep {
    private final Object context;

    protected AbstractNewProjectWizardStep() {
        this.context = null;
    }

    protected AbstractNewProjectWizardStep(Object context) {
        this.context = context;
    }

    public Object getContext() {
        return context;
    }
}
