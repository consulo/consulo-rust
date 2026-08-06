package com.intellij.openapi.externalSystem.autoimport;
public interface ExternalSystemProjectNotificationAware {
    default void onProjectReloadStart() {}
    default void onProjectReloadFinish(boolean success) {}
}
