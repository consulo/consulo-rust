package com.intellij.ide.impl;

import consulo.project.Project;

/** Stub for IJ's TrustStateListener. Consulo has no trust state — this listener is never fired. */
public interface TrustStateListener {
    default void onProjectTrusted(Project project) {}
    default void onProjectUntrusted(Project project) {}
    default void onProjectTrustedFromNotification(Project project) {}
}
