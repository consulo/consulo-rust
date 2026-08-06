package com.intellij.openapi.externalSystem.autoimport;
public interface ExternalSystemSettingsFilesModificationContext {
    default boolean isExplicitReload() { return false; }
    default ReloadStatus getReloadStatus() { return ReloadStatus.IDLE; }
    default ExternalSystemModificationType getModificationType() { return ExternalSystemModificationType.INTERNAL; }
    default Event getEvent() { return Event.UPDATE; }
    enum ReloadStatus { IDLE, IN_PROGRESS, JUST_FINISHED }
    enum Event { CREATE, UPDATE, DELETE }
}
