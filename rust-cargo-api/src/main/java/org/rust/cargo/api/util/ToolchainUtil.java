/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.cargo.api.util;


import consulo.util.lang.SemVer;
import org.rust.stdext.Utils;

import java.nio.file.Path;
import consulo.platform.Platform;
import org.rust.stdext.PathUtil;

public final class ToolchainUtil {

    private ToolchainUtil() {
    }

    public static SemVer parseSemVer(String version) {
        SemVer result = SemVer.parseFromText(version);
        if (result == null) {
            throw new IllegalStateException("Invalid version value: " + version);
        }
        return result;
    }

    public static boolean hasExecutable(Path path, String toolName) {
        return PathUtil.isExecutable(pathToExecutable(path, toolName));
    }

    /** The tool inside {@code path} itself, with no fall back to {@code PATH}. */
    public static boolean hasLocalExecutable(@jakarta.annotation.Nullable Path path, String toolName) {
        if (path == null) return false;
        String exeName = Platform.current().os().isWindows() ? toolName + ".exe" : toolName;
        return PathUtil.isExecutable(path.resolve(exeName).toAbsolutePath());
    }

    /**
     * The tool inside {@code path}, or the one on {@code PATH} when the toolchain does not carry it.
     * A toolchain directory holds what belongs to that toolchain - cargo, rustc, rustdoc - while
     * {@code rustup} manages all of them and is installed once, somewhere else entirely.
     */
    public static Path pathToExecutable(Path path, String toolName) {
        String exeName = Platform.current().os().isWindows() ? toolName + ".exe" : toolName;
        Path inToolchain = path.resolve(exeName).toAbsolutePath();
        if (PathUtil.isExecutable(inToolchain)) {
            return inToolchain;
        }
        Path onPath = findOnPath(exeName);
        return onPath != null ? onPath : inToolchain;
    }

    @jakarta.annotation.Nullable
    private static Path findOnPath(String exeName) {
        String pathEnv = Platform.current().os().getEnvironmentVariable("PATH");
        if (pathEnv == null || pathEnv.isEmpty()) return null;
        for (String entry : pathEnv.split(java.io.File.pathSeparator)) {
            if (entry.isEmpty()) continue;
            try {
                Path candidate = Path.of(entry).resolve(exeName);
                if (PathUtil.isExecutable(candidate)) return candidate.toAbsolutePath();
            }
            catch (java.nio.file.InvalidPathException ignored) {
            }
        }
        return null;
    }
}
