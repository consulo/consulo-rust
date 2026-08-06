/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.debugger;
import consulo.ide.impl.idea.ide.plugins.PluginManagerCore;

import consulo.container.plugin.PluginDescriptor;
import consulo.container.plugin.PluginManager;
import consulo.container.plugin.PluginId;
import jakarta.annotation.Nullable;

public final class DebuggerUtils {

    public static final PluginId NATIVE_DEBUGGING_SUPPORT_PLUGIN_ID = PluginId.getId("com.intellij.nativeDebug");

    private DebuggerUtils() {
    }

    @Nullable
    public static PluginDescriptor nativeDebuggingSupportPlugin() {
        return PluginManager.findPlugin(NATIVE_DEBUGGING_SUPPORT_PLUGIN_ID);
    }
}
