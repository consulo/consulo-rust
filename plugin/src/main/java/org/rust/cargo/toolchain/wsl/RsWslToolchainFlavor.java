/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.cargo.toolchain.wsl;

import com.intellij.execution.wsl.WSLDistribution;
import jakarta.annotation.Nonnull;
import org.rust.cargo.toolchain.flavors.RsToolchainFlavor;

import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;

/**
 * WSL toolchain flavor — Consulo doesn't ship with WSL support, so this flavor is disabled.
 */
public class RsWslToolchainFlavor extends RsToolchainFlavor {

    @Nonnull
    @Override
    protected Stream<Path> getHomePathCandidates() {
        return Stream.empty();
    }

    @Override
    protected boolean isApplicable() {
        return false;
    }

    @Override
    protected boolean isValidToolchainPath(@Nonnull Path path) {
        return false;
    }

    @Override
    protected boolean hasExecutable(@Nonnull Path path, @Nonnull String toolName) {
        return false;
    }

    @Nonnull
    @Override
    protected Path pathToExecutable(@Nonnull Path path, @Nonnull String toolName) {
        return path.resolve(toolName);
    }

    @Nonnull
    public static List<Path> getDistributionHomePathCandidates(@Nonnull WSLDistribution distribution) {
        return Collections.emptyList();
    }
}
