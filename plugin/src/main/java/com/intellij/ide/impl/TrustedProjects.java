package com.intellij.ide.impl;

import consulo.project.Project;

/** Minimal stub — Consulo has no Trusted-Projects mechanism; treat every project as trusted. */
public final class TrustedProjects {
    private TrustedProjects() {}
    public static boolean isTrusted(Project project) { return true; }
    public static boolean isTrustedCheckFromDialog(Project project) { return true; }
    public static boolean confirmLoadingUntrustedProject(Project project,
                                                         String title,
                                                         String message,
                                                         String trustButton,
                                                         String distrustButton) { return true; }
    public static boolean confirmLoadingUntrustedProject(Project project, Object... ignored) { return true; }
}
