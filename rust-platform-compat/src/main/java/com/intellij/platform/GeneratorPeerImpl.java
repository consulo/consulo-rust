package com.intellij.platform;
import javax.swing.JComponent;
import javax.swing.JPanel;
/** Default {@link ProjectGeneratorPeer}: holds the settings object and an empty UI component. */
public class GeneratorPeerImpl<T> implements ProjectGeneratorPeer<T> {
    private final T settings;
    public GeneratorPeerImpl() { this.settings = null; }
    public GeneratorPeerImpl(T settings, JComponent component) { this.settings = settings; }
    @Override public JComponent getComponent() { return new JPanel(); }
    @Override public T getSettings() { return settings; }
    @Override public void buildUI(Object panel) {}
    @Override public Object validate() { return null; }
    @Override public boolean isBackgroundJobRunning() { return false; }
    @Override public void addSettingsListener(Object listener) {}
    public interface SettingsListener {}
}
