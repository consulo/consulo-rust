package com.intellij.ide.plugins;
import consulo.container.plugin.PluginDescriptor;
public interface DynamicPluginListener {
    default void beforePluginLoaded(PluginDescriptor pluginDescriptor) {}
    default void pluginLoaded(PluginDescriptor pluginDescriptor) {}
    default void beforePluginUnload(PluginDescriptor pluginDescriptor, boolean isUpdate) {}
    default void pluginUnloaded(PluginDescriptor pluginDescriptor, boolean isUpdate) {}
    default void checkUnloadPlugin(PluginDescriptor pluginDescriptor) throws CannotUnloadPluginException {}
}
