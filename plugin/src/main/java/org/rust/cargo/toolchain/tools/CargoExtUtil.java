/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.cargo.toolchain.tools;

import jakarta.annotation.Nonnull;
import org.rust.cargo.toolchain.RsToolchainBase;

public final class CargoExtUtil {
    private CargoExtUtil() {
    }

    @Nonnull
    public static Cargo cargo(@Nonnull RsToolchainBase toolchain) {
        return new Cargo(toolchain);
    }
}
