/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.cargo.toolchain.flavors;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ExtensionAPI;
import consulo.component.extension.ExtensionPointName;
import java.nio.file.Files;
import jakarta.annotation.Nullable;
import org.rust.cargo.toolchain.tools.Cargo;
import org.rust.cargo.toolchain.tools.Rustc;
import org.rust.cargo.api.util.ToolchainUtil;

import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@ExtensionAPI(ComponentScope.APPLICATION)
public abstract class RsToolchainFlavor {

    public Stream<Path> suggestHomePaths() {
        return getHomePathCandidates().filter(this::isValidToolchainPath);
    }

    protected abstract Stream<Path> getHomePathCandidates();

    /**
     * Flavor is added to result in getApplicableFlavors if this method returns true.
     * @return whether this flavor is applicable.
     */
    protected boolean isApplicable() {
        return true;
    }

    /**
     * Checks if the path is the name of a Rust toolchain of this flavor.
     *
     * @param path path to check.
     * @return true if paths points to a valid home.
     */
    protected boolean isValidToolchainPath(Path path) {
        return Files.isDirectory(path) &&
            hasExecutable(path, Rustc.NAME) &&
            hasExecutable(path, Cargo.NAME);
    }

    protected boolean hasExecutable(Path path, String toolName) {
        return ToolchainUtil.hasExecutable(path, toolName);
    }

    protected Path pathToExecutable(Path path, String toolName) {
        return ToolchainUtil.pathToExecutable(path, toolName);
    }

    private static final ExtensionPointName<RsToolchainFlavor> EP_NAME =
        ExtensionPointName.create(RsToolchainFlavor.class);

    public static List<RsToolchainFlavor> getApplicableFlavors() {
        return EP_NAME.getExtensionList().stream()
            .filter(RsToolchainFlavor::isApplicable)
            .collect(Collectors.toList());
    }

    @Nullable
    public static RsToolchainFlavor getFlavor(Path path) {
        return getApplicableFlavors().stream()
            .filter(flavor -> flavor.isValidToolchainPath(path))
            .findFirst()
            .orElse(null);
    }
}
