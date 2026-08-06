package com.intellij.ide.plugins;
public final class InstalledPluginsState {
    public static InstalledPluginsState getInstance() { return new InstalledPluginsState(); }
    public boolean isRestartRequired() { return false; }
}
