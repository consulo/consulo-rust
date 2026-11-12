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
        return hasExecutable(Platform.current(), path, toolName);
    }

    /**
     * Whether the tool is found for {@code platform} - inside {@code path}, or on that machine's {@code PATH}.
     */
    public static boolean hasExecutable(Platform platform, Path path, String toolName) {
        return PathUtil.isExecutable(pathToExecutable(platform, path, toolName));
    }

    /** The tool inside {@code path} itself, with no fall back to {@code PATH}. */
    public static boolean hasLocalExecutable(@jakarta.annotation.Nullable Path path, String toolName) {
        return hasExecutableIn(Platform.current(), path, toolName);
    }

    /** The tool inside {@code path} of {@code platform} itself, with no fall back to {@code PATH}. */
    public static boolean hasExecutableIn(Platform platform, @jakarta.annotation.Nullable Path path, String toolName) {
        if (path == null) return false;
        return PathUtil.isExecutable(path.resolve(executableName(platform, toolName)).toAbsolutePath());
    }

    /**
     * The file name of a tool on {@code platform} - the operating system of that machine decides, not the one of the IDE.
     */
    public static String executableName(Platform platform, String toolName) {
        return platform.os().isWindows() ? toolName + ".exe" : toolName;
    }

    /**
     * The tool inside {@code path}, or the one on {@code PATH} when the toolchain does not carry it.
     * A toolchain directory holds what belongs to that toolchain - cargo, rustc, rustdoc - while
     * {@code rustup} manages all of them and is installed once, somewhere else entirely.
     */
    public static Path pathToExecutable(Path path, String toolName) {
        return pathToExecutable(Platform.current(), path, toolName);
    }

    /**
     * Like {@link #pathToExecutable(Path, String)} for a toolchain of {@code platform} - its {@code PATH} is the one of
     * that machine.
     */
    public static Path pathToExecutable(Platform platform, Path path, String toolName) {
        String exeName = executableName(platform, toolName);
        Path inToolchain = path.resolve(exeName).toAbsolutePath();
        if (PathUtil.isExecutable(inToolchain)) {
            return inToolchain;
        }
        Path onPath = platform.findExecutableInPath(exeName);
        return onPath != null ? onPath : inToolchain;
    }
}
