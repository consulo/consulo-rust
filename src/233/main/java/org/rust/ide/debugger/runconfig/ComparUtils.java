/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.debugger.runconfig;

import com.intellij.ide.plugins.IdeaPluginDescriptor;
import com.intellij.ide.plugins.PluginManagerCore;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public final class ComparUtils {

    private ComparUtils() {
    }

    @NotNull
    public static List<IdeaPluginDescriptor> getLoadedPlugins(@NotNull PluginManagerCore pluginManagerCore) {
        return pluginManagerCore.getLoadedPlugins();
    }
}
