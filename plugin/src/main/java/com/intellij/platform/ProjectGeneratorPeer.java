package com.intellij.platform;
import javax.swing.JComponent;
/** IntelliJ-compat stub for platform new-project wizard peer. */
public interface ProjectGeneratorPeer<T> {
    JComponent getComponent();
    T getSettings();
    void buildUI(Object settingsStep);
    Object validate();
    default boolean isBackgroundJobRunning() { return false; }
    default void addSettingsListener(Object listener) {}
}
