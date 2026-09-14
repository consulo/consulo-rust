/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.cargo.toolchain;

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
}
