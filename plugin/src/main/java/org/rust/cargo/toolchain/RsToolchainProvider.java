/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.cargo.toolchain;

import consulo.platform.Platform;
import jakarta.annotation.Nullable;

import java.nio.file.Files;
import java.nio.file.Path;

public interface RsToolchainProvider {
    @Nullable
    static RsToolchainBase getToolchainStatic(Path homePath) {
        if (Files.exists(homePath)) {
            return new RsLocalToolchain(homePath);
        }
        return null;
    }

    /**
     * A toolchain of {@code platform} - {@code homePath} is a path of that platform.
     */
    @Nullable
    static RsToolchainBase getToolchainStatic(Path homePath, Platform platform) {
        if (Files.exists(homePath)) {
            return new RsLocalToolchain(homePath, platform);
        }
        return null;
    }
}
