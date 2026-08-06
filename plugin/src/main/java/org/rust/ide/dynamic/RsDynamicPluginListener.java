/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.dynamic;

import com.intellij.ide.plugins.CannotUnloadPluginException;
import com.intellij.ide.plugins.DynamicPluginListener;
import consulo.container.plugin.PluginDescriptor;
import consulo.container.plugin.PluginId;
import org.rust.openapiext.OpenApiUtil;

public class RsDynamicPluginListener implements DynamicPluginListener {
    @Override
    public void checkUnloadPlugin(PluginDescriptor pluginDescriptor) throws CannotUnloadPluginException {
        if (pluginDescriptor.getPluginId().equals(PluginId.findId(OpenApiUtil.PLUGIN_ID))) {
            // See https://github.com/intellij-rust/intellij-rust/issues/4832
            throw new CannotUnloadPluginException("Rust plugin cannot be dynamically unloaded for now");
        }
    }
}
