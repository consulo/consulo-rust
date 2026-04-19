/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.cargo.toolchain.tools;

import org.jetbrains.annotations.NotNull;
import org.rust.cargo.toolchain.RsToolchainBase;

public final class CargoExtUtil {
    private CargoExtUtil() {
    }

    @NotNull
    public static Cargo cargo(@NotNull RsToolchainBase toolchain) {
        return new Cargo(toolchain);
    }
}
