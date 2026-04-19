/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.debugger;

import com.intellij.ide.plugins.IdeaPluginDescriptor;
import com.intellij.ide.plugins.PluginManagerCore;
import com.intellij.openapi.extensions.PluginId;
import org.jetbrains.annotations.Nullable;

public final class DebuggerUtils {

    public static final PluginId NATIVE_DEBUGGING_SUPPORT_PLUGIN_ID = PluginId.getId("com.intellij.nativeDebug");

    private DebuggerUtils() {
    }

    @Nullable
    public static IdeaPluginDescriptor nativeDebuggingSupportPlugin() {
        return PluginManagerCore.getPlugin(NATIVE_DEBUGGING_SUPPORT_PLUGIN_ID);
    }
}
