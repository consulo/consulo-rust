/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.cargo.toolchain.wsl;

import com.intellij.execution.wsl.WSLDistribution;
import org.jetbrains.annotations.NotNull;

import java.nio.file.Path;

public final class Utils {
    private Utils() {
    }

    @NotNull
    public static String expandUserHome(@NotNull WSLDistribution distribution, @NotNull String path) {
        if (!path.startsWith("~/")) return path;
        String userHome = distribution.getUserHome();
        if (userHome == null) return path;
        return userHome + path.substring(1);
    }

    public static boolean hasExecutableOnWsl(@NotNull Path path, @NotNull String toolName) {
        return pathToExecutableOnWsl(path, toolName).toFile().isFile();
    }

    @NotNull
    public static Path pathToExecutableOnWsl(@NotNull Path path, @NotNull String toolName) {
        return path.resolve(toolName);
    }
}
