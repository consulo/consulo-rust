package com.intellij.ide.wizard;

/** Base class for a new-project wizard step; carries the shared wizard context. */
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
