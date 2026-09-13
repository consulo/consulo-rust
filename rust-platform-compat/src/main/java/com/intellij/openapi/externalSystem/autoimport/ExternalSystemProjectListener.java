package com.intellij.openapi.externalSystem.autoimport;
public interface ExternalSystemProjectListener {
    default void onProjectReloadStart() {}
    default void onProjectReloadFinish(boolean success) {}
}
