/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.cargo.toolchain.wsl;

import com.intellij.execution.wsl.WSLDistribution;
import jakarta.annotation.Nonnull;

import java.nio.file.Path;

public final class WslUtilsUtil {

    private WslUtilsUtil() {
    }

    @Nonnull
    public static String expandUserHome(@Nonnull WSLDistribution distribution, @Nonnull String path) {
        if (!path.startsWith("~/")) return path;
        String userHome = distribution.getUserHome();
        if (userHome == null) return path;
        return userHome + path.substring(1);
    }

    public static boolean hasExecutableOnWsl(@Nonnull Path path, @Nonnull String toolName) {
        return pathToExecutableOnWsl(path, toolName).toFile().isFile();
    }

    @Nonnull
    public static Path pathToExecutableOnWsl(@Nonnull Path path, @Nonnull String toolName) {
        return path.resolve(toolName);
    }
}
