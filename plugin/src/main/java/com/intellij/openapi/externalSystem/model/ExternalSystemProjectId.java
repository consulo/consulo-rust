package com.intellij.openapi.externalSystem.model;

import consulo.externalSystem.model.ProjectSystemId;

/** IntelliJ-compat stub — identifies an external-system project. */
public final class ExternalSystemProjectId {
    private final ProjectSystemId systemId;
    private final String externalProjectPath;
    public ExternalSystemProjectId(ProjectSystemId systemId, String externalProjectPath) {
        this.systemId = systemId;
        this.externalProjectPath = externalProjectPath;
    }
    public ExternalSystemProjectId(String systemId, String externalProjectPath) {
        this.systemId = new ProjectSystemId(systemId);
        this.externalProjectPath = externalProjectPath;
    }
    public ProjectSystemId getSystemId() { return systemId; }
    public String getExternalProjectPath() { return externalProjectPath; }
    public String getProjectName() { return externalProjectPath; }
}
