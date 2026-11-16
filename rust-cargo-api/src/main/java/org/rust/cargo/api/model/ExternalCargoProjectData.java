/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.cargo.api.model;

import org.jspecify.annotations.Nullable;
import org.rust.cargo.api.CfgOptions;
import org.rust.cargo.api.workspace.CargoWorkspaceData;

import java.nio.file.Path;
import java.util.Objects;

/**
 * A workspace built by another build system (Bazel, Buck, ...), handed to
 * {@link CargoProjectsService#attachExternalCargoProject} instead of being read from {@code cargo metadata}.
 * <p>
 * The workspace is described with the same data {@code cargo metadata} is translated into:
 * <ul>
 *   <li>{@link CargoWorkspaceData.Package} - one per crate owner: {@code contentRootUrl}, name, version, the
 *       {@link org.rust.cargo.api.workspace.PackageOrigin origin} ({@code WORKSPACE} for the crates of the project,
 *       {@code DEPENDENCY} for third party ones), edition, {@code features} / {@code enabledFeatures}, the package's own
 *       {@code cfgOptions}, {@code env}, the build script {@code outDirUrl} and the {@code procMacroArtifact} (the built
 *       proc-macro dylib);</li>
 *   <li>{@link CargoWorkspaceData.Target} - one per crate: the {@code crateRootUrl}, name, kind and edition;</li>
 *   <li>{@link CargoWorkspaceData#getDependencies()} - the resolved dependencies between packages, by package id.</li>
 * </ul>
 * The standard library must not be part of the data, it is added by the sync from {@link ExternalRustToolchain}.
 * <p>
 * The plugin creates no modules for such a project: the build system that provides it owns the project model. The
 * dependency and standard library crates still become order entries of the module whose content holds
 * {@link #getManifest()}'s directory, and a file of a {@code WORKSPACE} package is attributed to the package whose
 * content root is its nearest ancestor.
 */
public final class ExternalCargoProjectData {
    private final Path myManifest;
    private final CargoWorkspaceData myWorkspaceData;
    private final @Nullable CfgOptions myCfgOptions;
    private final @Nullable ExternalRustToolchain myToolchain;

    /**
     * @param manifest      the identity of the project, and its directory the project root - the build file of the
     *                      build system (e.g. Bazel's {@code MODULE.bazel}), never a {@code Cargo.toml}
     * @param workspaceData the crates of the project and their dependencies
     * @param cfgOptions    the cfg options of the compilation target, {@code null} to ask the toolchain's
     *                      {@code rustc --print cfg}
     * @param toolchain     the toolchain the crates are built with, {@code null} to use the Rust bundle of the project
     */
    public ExternalCargoProjectData(
        Path manifest,
        CargoWorkspaceData workspaceData,
        @Nullable CfgOptions cfgOptions,
        @Nullable ExternalRustToolchain toolchain
    ) {
        myManifest = manifest;
        myWorkspaceData = workspaceData;
        myCfgOptions = cfgOptions;
        myToolchain = toolchain;
    }

    public Path getManifest() {
        return myManifest;
    }

    public CargoWorkspaceData getWorkspaceData() {
        return myWorkspaceData;
    }

    public @Nullable CfgOptions getCfgOptions() {
        return myCfgOptions;
    }

    public @Nullable ExternalRustToolchain getToolchain() {
        return myToolchain;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof ExternalCargoProjectData that)) {
            return false;
        }
        return myManifest.equals(that.myManifest)
            && myWorkspaceData.equals(that.myWorkspaceData)
            && Objects.equals(myCfgOptions, that.myCfgOptions)
            && Objects.equals(myToolchain, that.myToolchain);
    }

    @Override
    public int hashCode() {
        return Objects.hash(myManifest, myWorkspaceData, myCfgOptions, myToolchain);
    }

    @Override
    public String toString() {
        return "ExternalCargoProjectData(manifest=" + myManifest + ", toolchain=" + myToolchain + ")";
    }
}
