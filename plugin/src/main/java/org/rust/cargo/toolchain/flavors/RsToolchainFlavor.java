/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.cargo.toolchain.flavors;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ExtensionAPI;
import consulo.component.extension.ExtensionPointName;
import consulo.platform.Platform;
import jakarta.annotation.Nullable;
import org.rust.cargo.api.util.ToolchainUtil;
import org.rust.cargo.toolchain.tools.Cargo;
import org.rust.cargo.toolchain.tools.Rustc;
import org.rust.stdext.PathUtil;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * A place a Rust toolchain is installed at on a machine - the local one or a remote platform. Everything (environment,
 * user home, paths, operating system) is read from the given {@link Platform}, never from the machine of the IDE.
 */
@ExtensionAPI(ComponentScope.APPLICATION)
public abstract class RsToolchainFlavor {
    public Stream<Path> suggestHomePaths(Platform platform) {
        return getHomePathCandidates(platform).filter(path -> isValidToolchainPath(platform, path));
    }

    protected abstract Stream<Path> getHomePathCandidates(Platform platform);

    /**
     * Flavor is added to result in getApplicableFlavors if this method returns true.
     * @return whether this flavor is applicable.
     */
    protected boolean isApplicable(Platform platform) {
        return true;
    }

    /**
     * Checks if the path is the name of a Rust toolchain of this flavor.
     *
     * @param path path to check.
     * @return true if paths points to a valid home.
     */
    protected boolean isValidToolchainPath(Platform platform, Path path) {
        return Files.isDirectory(path) &&
            hasExecutable(platform, path, Rustc.NAME) &&
            hasExecutable(platform, path, Cargo.NAME);
    }

    /**
     * The tool inside {@code path} itself - a directory is a toolchain by what it holds, not by what is on PATH.
     */
    protected boolean hasExecutable(Platform platform, Path path, String toolName) {
        return ToolchainUtil.hasExecutableIn(platform, path, toolName);
    }

    protected Path pathToExecutable(Platform platform, Path path, String toolName) {
        return ToolchainUtil.pathToExecutable(platform, path, toolName);
    }

    private static final ExtensionPointName<RsToolchainFlavor> EP_NAME =
        ExtensionPointName.create(RsToolchainFlavor.class);

    public static List<RsToolchainFlavor> getApplicableFlavors(Platform platform) {
        return EP_NAME.getExtensionList().stream()
            .filter(flavor -> flavor.isApplicable(platform))
            .collect(Collectors.toList());
    }

    @Nullable
    public static RsToolchainFlavor getFlavor(Platform platform, Path path) {
        return getApplicableFlavors(platform).stream()
            .filter(flavor -> flavor.isValidToolchainPath(platform, path))
            .findFirst()
            .orElse(null);
    }

    /**
     * A tool which is not part of a toolchain - {@code rustup}, or one installed by {@code cargo install} - in the places
     * the flavors know of on {@code platform} (cargo home, PATH, ...).
     */
    @Nullable
    public static Path findExecutable(Platform platform, String toolName) {
        String exeName = ToolchainUtil.executableName(platform, toolName);
        for (RsToolchainFlavor flavor : getApplicableFlavors(platform)) {
            Path found = flavor.getHomePathCandidates(platform)
                .map(dir -> dir.resolve(exeName))
                .filter(PathUtil::isExecutable)
                .findFirst()
                .orElse(null);
            if (found != null) {
                return found;
            }
        }
        return null;
    }
}
