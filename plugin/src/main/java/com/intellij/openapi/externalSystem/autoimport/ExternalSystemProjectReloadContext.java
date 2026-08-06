package com.intellij.openapi.externalSystem.autoimport;
public interface ExternalSystemProjectReloadContext {
    default boolean isExplicitReload() { return false; }
    enum ReloadStatus { IDLE, IN_PROGRESS, JUST_FINISHED }
}
