/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.cargo.toolchain;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.rust.openapiext.RsPathManager;

import java.nio.file.Path;

/**
 * Locates the proc-macro expander binary for a toolchain: the one shipped with the toolchain if it
 * has one, otherwise the helper bundled with the plugin.
 */
public final class ProcMacroExpanderPath {
    private ProcMacroExpanderPath() {
    }

    @Nullable
    public static Path find(@Nonnull RsToolchainBase toolchain, @Nonnull String sysroot) {
        Path fromToolchain = findInToolchain(toolchain, sysroot);
        return fromToolchain != null ? fromToolchain : findEmbedded(toolchain);
    }

    @Nullable
    private static Path findInToolchain(@Nonnull RsToolchainBase toolchain, @Nonnull String sysroot) {
        String binaryName = toolchain.getExecutableName("rust-analyzer-proc-macro-srv");
        Path expanderPath = Path.of(sysroot, "libexec", binaryName);

        if (!expanderPath.toFile().canExecute()) {
            return null;
        }
        return expanderPath;
    }

    @Nullable
    private static Path findEmbedded(@Nonnull RsToolchainBase toolchain) {
        return RsPathManager.INSTANCE.nativeHelper(false);
    }
}
