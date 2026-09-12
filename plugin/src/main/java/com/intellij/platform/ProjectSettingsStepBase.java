package com.intellij.platform;

/** Wizard step showing the settings panel of a directory project generator. */
public class ProjectSettingsStepBase<T> extends com.intellij.openapi.wm.impl.welcomeScreen.AbstractActionWithPanel {
    public ProjectSettingsStepBase(DirectoryProjectGenerator<T> projectGenerator, Object callback) {}
    @Override public javax.swing.JPanel createPanel() { return new javax.swing.JPanel(); }
}
