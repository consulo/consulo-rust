package com.intellij.ide.util.projectWizard;

import consulo.project.Project;

/** IntelliJ-compat stub. Consulo has a different new-project wizard API. */
public class WizardContext {
    private Project project;
    private Object projectBuilder;

    public Project getProject() { return project; }
    public void setProject(Project project) { this.project = project; }
    public String getProjectFileDirectory() { return null; }

    public Object getProjectBuilder() { return projectBuilder; }
    public void setProjectBuilder(Object projectBuilder) { this.projectBuilder = projectBuilder; }
}
