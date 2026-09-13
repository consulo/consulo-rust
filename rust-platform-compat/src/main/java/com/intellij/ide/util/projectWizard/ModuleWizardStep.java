package com.intellij.ide.util.projectWizard;

import javax.swing.JComponent;
import consulo.configurable.ConfigurationException;

/** One step of the new-module wizard: its UI, validation and data-model updates. */
public abstract class ModuleWizardStep {
    public abstract JComponent getComponent();
    public void updateDataModel() {}
    public void disposeUIResources() {}
    public void updateStep() {}
    public boolean validate() throws consulo.configurable.ConfigurationException { return true; }
    public boolean isStepVisible() { return true; }
    public String getName() { return ""; }
    public String getHelpId() { return null; }
    public void onStepLeaving() {}
    public void onWizardFinished() {}
    public JComponent getPreferredFocusedComponent() { return getComponent(); }
}
