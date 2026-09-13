package com.intellij.openapi.externalSystem.autoimport;

import com.intellij.openapi.externalSystem.model.ExternalSystemProjectId;
import consulo.disposer.Disposable;
import consulo.externalSystem.model.ProjectSystemId;

import java.util.Set;

/** External-system auto-reload integration point. */
public interface ExternalSystemProjectAware {
    default ProjectSystemId getSystemId() { return getProjectId().getSystemId(); }
    ExternalSystemProjectId getProjectId();
    Set<String> getSettingsFiles();
    default boolean isIgnoredSettingsFileEvent(String path, ExternalSystemSettingsFilesModificationContext context) { return false; }
    default void subscribe(ExternalSystemProjectListener listener, Disposable parentDisposable) {}
    default void reloadProject(ExternalSystemProjectReloadContext context) {}
}
