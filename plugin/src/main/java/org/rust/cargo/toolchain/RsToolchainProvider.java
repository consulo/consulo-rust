/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.cargo.toolchain;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ExtensionAPI;
import consulo.component.extension.ExtensionPointName;
import jakarta.annotation.Nullable;

import java.nio.file.Path;

@ExtensionAPI(ComponentScope.APPLICATION)
public interface RsToolchainProvider {

    @Nullable
    RsToolchainBase getToolchain(Path homePath);

    ExtensionPointName<RsToolchainProvider> EP_NAME =
        ExtensionPointName.create(RsToolchainProvider.class);

    @Nullable
    static RsToolchainBase getToolchainStatic(Path homePath) {
        for (RsToolchainProvider provider : EP_NAME.getExtensionList()) {
            RsToolchainBase toolchain = provider.getToolchain(homePath);
            if (toolchain != null) {
                return toolchain;
            }
        }
        return null;
    }
}
