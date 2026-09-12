package com.intellij.platform;
import javax.swing.JComponent;
/** Settings UI a project generator contributes to the new-project wizard. */
public interface ProjectGeneratorPeer<T> {
    JComponent getComponent();
    T getSettings();
    void buildUI(Object settingsStep);
    Object validate();
    default boolean isBackgroundJobRunning() { return false; }
    default void addSettingsListener(Object listener) {}
}
