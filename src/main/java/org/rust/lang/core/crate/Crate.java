/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.crate;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.UserDataHolderEx;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.rust.cargo.CfgOptions;
import org.rust.cargo.project.model.CargoProject;
import org.rust.cargo.project.workspace.CargoWorkspace;
import org.rust.cargo.project.workspace.CargoWorkspaceData;
import org.rust.cargo.project.workspace.FeatureState;
import org.rust.cargo.project.workspace.PackageOrigin;
import org.rust.lang.core.crate.impl.FakeCrate;
import org.rust.lang.core.psi.RsFile;

import java.util.*;

/**
 * An immutable object describes a <i>crate</i> from the <i>rustc</i> point of view.
 * In Cargo-based project this is usually a wrapper around {@link CargoWorkspace.Target}
 */
public interface Crate extends UserDataHolderEx {
    /**
     * This id can be saved to a disk and then used to find the crate via {@link CrateGraphService#findCrateById}.
     * Can be {@code null} for crates that are not represented in the physical filesystem and can't be retrieved
     * using {@link CrateGraphService#findCrateById}, or for invalid crates (without a root module)
     */
    @Nullable
    Integer getId();

    @NotNull
    CargoWorkspace.Edition getEdition();

    @Nullable
    CargoProject getCargoProject();

    @Nullable
    CargoWorkspace getCargoWorkspace();

    @Nullable
    CargoWorkspace.Target getCargoTarget();

    @NotNull
    CargoWorkspace.TargetKind getKind();

    @NotNull
    PackageOrigin getOrigin();

    @NotNull
    CfgOptions getCfgOptions();

    @NotNull
    Map<String, FeatureState> getFeatures();

    /**
     * {@code true} if there isn't a custom build script ({@code build.rs}) in the package or if the build script is
     * successfully evaluated (hence {@link #getCfgOptions()} is filled). The value affects {@code #[cfg()]} and
     * {@code #[cfg_attr()]} attributes evaluation.
     */
    boolean getEvaluateUnknownCfgToFalse();

    /** A map of compile-time environment variables, needed for {@code env!("FOO")} macros expansion */
    @NotNull
    Map<String, String> getEnv();

    /** Represents {@code OUT_DIR} compile-time environment variable. Used for {@code env!("OUT_DIR")} macros expansion */
    @Nullable
    VirtualFile getOutDir();

    /** Direct dependencies */
    @NotNull
    Collection<Dependency> getDependencies();

    /** All dependencies (including transitive) of this crate. Topological sorted */
    @NotNull
    LinkedHashSet<Crate> getFlatDependencies();

    /** Other crates that depends on this crate */
    @NotNull
    List<Crate> getReverseDependencies();

    /**
     * A cargo package can have cyclic dependencies through {@code [dev-dependencies]} (see {@link CrateGraphService} docs).
     * Cyclic dependencies are not contained in {@link #getDependencies()}, {@link #getFlatDependencies()} or {@link #getReverseDependencies()}.
     */
    @NotNull
    default Collection<Dependency> getDependenciesWithCyclic() {
        return getDependencies();
    }

    /** A cargo package can have cyclic dependencies through {@code [dev-dependencies]} (see {@link CrateGraphService} docs) */
    default boolean getHasCyclicDevDependencies() {
        return false;
    }

    /**
     * A root module of the crate, also known as "crate root". Usually it's {@code main.rs} or {@code lib.rs}.
     * Use carefully: can be null or invalid ({@link VirtualFile#isValid()})
     */
    @Nullable
    VirtualFile getRootModFile();

    @Nullable
    RsFile getRootMod();

    boolean getAreDoctestsEnabled();

    /** A name to display to a user */
    @NotNull
    String getPresentableName();

    /**
     * A name that can be used as a valid Rust identifier. Usually it is {@link #getPresentableName()} with "-" chars
     * replaced to "_".
     *
     * NOTE that Rust crate doesn't have any kind of "global" name. The actual crate name can be different
     * in a particular dependent crate. Use {@link Dependency#getNormName()} instead
     */
    @NotNull
    String getNormName();

    @NotNull
    Project getProject();

    /**
     * A procedural macro compiler artifact (compiled binary).
     * Non-null only if this crate is a procedural macro, the crate is successfully compiled during
     * the Cargo sync phase and the experimental feature is enabled.
     */
    @Nullable
    CargoWorkspaceData.ProcMacroArtifact getProcMacroArtifact();

    class Dependency {
        /** A name of the dependency that can be used in {@code extern crate name;} or in absolute paths */
        @NotNull
        private final String normName;

        @NotNull
        private final Crate crate;

        public Dependency(@NotNull String normName, @NotNull Crate crate) {
            this.normName = normName;
            this.crate = crate;
        }

        @NotNull
        public String getNormName() {
            return normName;
        }

        @NotNull
        public Crate getCrate() {
            return crate;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Dependency that = (Dependency) o;
            return normName.equals(that.normName) && crate.equals(that.crate);
        }

        @Override
        public int hashCode() {
            return Objects.hash(normName, crate);
        }

        @Override
        public String toString() {
            return "Dependency(normName=" + normName + ", crate=" + crate + ")";
        }
    }

    // Extension functions converted to static methods

    @Nullable
    static Crate findDependency(@NotNull Crate self, @NotNull String normName) {
        for (Dependency dep : self.getDependencies()) {
            if (dep.getNormName().equals(normName)) {
                return dep.getCrate();
            }
        }
        return null;
    }

    static boolean hasTransitiveDependencyOrSelf(@NotNull Crate self, @NotNull Crate other) {
        return other == self || self.getFlatDependencies().contains(other);
    }

    @Nullable
    static Crate asNotFake(@NotNull Crate self) {
        return self instanceof FakeCrate ? null : self;
    }
}
