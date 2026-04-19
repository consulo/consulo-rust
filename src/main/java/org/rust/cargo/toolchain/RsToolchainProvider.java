/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.cargo.toolchain;

import com.intellij.openapi.extensions.ExtensionPointName;
import org.jetbrains.annotations.Nullable;

import java.nio.file.Path;

public interface RsToolchainProvider {

    @Nullable
    RsToolchainBase getToolchain(Path homePath);

    ExtensionPointName<RsToolchainProvider> EP_NAME =
        ExtensionPointName.create("org.rust.toolchainProvider");

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
