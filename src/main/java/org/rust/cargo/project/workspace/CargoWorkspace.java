/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.cargo.project.workspace;

import com.intellij.openapi.util.UserDataHolderEx;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.util.ThreeState;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.TestOnly;
import org.rust.cargo.CargoConfig;
import org.rust.cargo.CfgOptions;
import org.rust.cargo.project.model.CargoProjectsService;
import org.rust.cargo.project.model.RustcInfo;
import org.rust.cargo.project.model.impl.UserDisabledFeatures;

import java.nio.file.Path;
import java.util.*;

/**
 * Rust project model represented roughly in the same way as in Cargo itself.
 * <p>
 * {@link CargoProjectsService} manages workspaces.
 */
public interface CargoWorkspace {
    Path getManifestPath();

    default Path getContentRoot() {
        return getManifestPath().getParent();
    }

    @Nullable
    VirtualFile getWorkspaceRoot();

    CfgOptions getCfgOptions();

    CargoConfig getCargoConfig();

    /**
     * Flatten list of packages including workspace members, dependencies, transitive dependencies
     * and stdlib. Use {@code packages.stream().filter(it -> it.getOrigin() == PackageOrigin.WORKSPACE)} to
     * obtain workspace members.
     */
    Collection<Package> getPackages();

    FeatureGraph getFeatureGraph();

    @Nullable
    default Package findPackageById(String id) {
        for (Package pkg : getPackages()) {
            if (pkg.getId().equals(id)) return pkg;
        }
        return null;
    }

    @Nullable
    default Package findPackageByName(String name, ThreeState isStd) {
        for (Package pkg : getPackages()) {
            if (!pkg.getName().equals(name) && !pkg.getNormName().equals(name)) continue;
            switch (isStd) {
                case YES:
                    if (pkg.getOrigin() == PackageOrigin.STDLIB) return pkg;
                    break;
                case NO:
                    if (pkg.getOrigin() == PackageOrigin.WORKSPACE || pkg.getOrigin() == PackageOrigin.DEPENDENCY) return pkg;
                    break;
                case UNSURE:
                    return pkg;
            }
        }
        return null;
    }

    @Nullable
    default Package findPackageByName(String name) {
        return findPackageByName(name, ThreeState.UNSURE);
    }

    @Nullable
    Target findTargetByCrateRoot(VirtualFile root);

    default boolean isCrateRoot(VirtualFile root) {
        return findTargetByCrateRoot(root) != null;
    }

    CargoWorkspace withStdlib(StandardLibrary stdlib, CfgOptions cfgOptions, @Nullable RustcInfo rustcInfo);

    default CargoWorkspace withStdlib(StandardLibrary stdlib, CfgOptions cfgOptions) {
        return withStdlib(stdlib, cfgOptions, null);
    }

    CargoWorkspace withDisabledFeatures(UserDisabledFeatures userDisabledFeatures);

    default boolean getHasStandardLibrary() {
        for (Package pkg : getPackages()) {
            if (pkg.getOrigin() == PackageOrigin.STDLIB) return true;
        }
        return false;
    }

    @TestOnly
    CargoWorkspace withImplicitDependency(CargoWorkspaceData.Package pkgToAdd);

    @TestOnly
    CargoWorkspace withEdition(Edition edition);

    @TestOnly
    CargoWorkspace withCfgOptions(CfgOptions cfgOptions);

    @TestOnly
    CargoWorkspace withCargoFeatures(Map<PackageFeature, List<String>> features);

    /** See docs for {@link CargoProjectsService} */
    interface Package extends UserDataHolderEx {
        @Nullable
        VirtualFile getContentRoot();

        Path getRootDirectory();

        String getId();

        String getName();

        default String getNormName() {
            return getName().replace('-', '_');
        }

        String getVersion();

        @Nullable
        String getSource();

        PackageOrigin getOrigin();

        Collection<Target> getTargets();

        @Nullable
        default Target getLibTarget() {
            for (Target t : getTargets()) {
                if (t.getKind().isLib()) return t;
            }
            return null;
        }

        @Nullable
        default Target getCustomBuildTarget() {
            for (Target t : getTargets()) {
                if (t.getKind() == TargetKind.CustomBuild.INSTANCE) return t;
            }
            return null;
        }

        default boolean getHasCustomBuildScript() {
            return getCustomBuildTarget() != null;
        }

        Collection<Dependency> getDependencies();

        /**
         * Cfg options from the package custom build script (build.rs). null if there isn't build script
         * or the build script was not evaluated successfully or build script evaluation is disabled
         */
        @Nullable
        CfgOptions getCfgOptions();

        Set<PackageFeature> getFeatures();

        CargoWorkspace getWorkspace();

        Edition getEdition();

        Map<String, String> getEnv();

        @Nullable
        VirtualFile getOutDir();

        Map<String, FeatureState> getFeatureState();

        @Nullable
        CargoWorkspaceData.ProcMacroArtifact getProcMacroArtifact();

        @Nullable
        default Target findDependency(String normName) {
            if (getNormName().equals(normName)) return getLibTarget();
            for (Dependency dep : getDependencies()) {
                if (dep.getName().equals(normName)) {
                    Package depPkg = dep.getPkg();
                    return depPkg.getLibTarget();
                }
            }
            return null;
        }
    }

    /** See docs for {@link CargoProjectsService} */
    interface Target {
        String getName();

        // target name must be a valid Rust identifier, so normalize it by mapping `-` to `_`
        default String getNormName() {
            return getName().replace('-', '_');
        }

        TargetKind getKind();

        @Nullable
        VirtualFile getCrateRoot();

        Package getPkg();

        Edition getEdition();

        boolean getDoctest();

        /** See {@link org.rust.cargo.toolchain.impl.CargoMetadata.Target#required_features} */
        List<String> getRequiredFeatures();

        /** Complete cfg options of the target. Combines compiler options, package options and target options */
        CfgOptions getCfgOptions();
    }

    interface Dependency {
        Package getPkg();

        String getName();

        String getCargoFeatureDependencyPackageName();

        List<DepKindInfo> getDepKinds();

        /**
         * Consider Cargo.toml:
         * <pre>
         * [dependencies.foo]
         * version = "*"
         * features = ["bar", "baz"]
         * </pre>
         * For dependency foo, features bar and baz are considered "required"
         */
        Set<String> getRequiredFeatures();
    }

    final class DepKindInfo {
        private final DepKind myKind;
        @Nullable
        private final String myTarget;

        public DepKindInfo(DepKind kind, @Nullable String target) {
            myKind = kind;
            myTarget = target;
        }

        public DepKindInfo(DepKind kind) {
            this(kind, null);
        }

        public DepKind getKind() { return myKind; }
        @Nullable public String getTarget() { return myTarget; }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof DepKindInfo)) return false;
            DepKindInfo that = (DepKindInfo) o;
            return myKind == that.myKind && Objects.equals(myTarget, that.myTarget);
        }

        @Override
        public int hashCode() {
            return Objects.hash(myKind, myTarget);
        }

        @Override
        public String toString() {
            return "DepKindInfo(kind=" + myKind + ", target=" + myTarget + ")";
        }
    }

    enum DepKind {
        // For old Cargo versions prior to 1.41.0
        Unclassified(null),
        Stdlib("stdlib?"),
        // [dependencies]
        Normal(null),
        // [dev-dependencies]
        Development("dev"),
        // [build-dependencies]
        Build("build");

        @Nullable
        private final String myCargoName;

        DepKind(@Nullable String cargoName) {
            myCargoName = cargoName;
        }

        @Nullable
        public String getCargoName() {
            return myCargoName;
        }
    }

    abstract class TargetKind {
        private final String myName;

        protected TargetKind(String name) {
            myName = name;
        }

        public String getName() {
            return myName;
        }

        public boolean isLib() { return this instanceof Lib; }
        public boolean isBin() { return this == Bin.INSTANCE; }
        public boolean isExampleBin() { return this == ExampleBin.INSTANCE; }
        public boolean isCustomBuild() { return this == CustomBuild.INSTANCE; }

        public boolean isProcMacro() {
            return this instanceof Lib && ((Lib) this).getKinds().contains(LibKind.PROC_MACRO);
        }

        public boolean canHaveMainFunction() {
            return isBin() || isExampleBin() || isCustomBuild();
        }

        public static final class Lib extends TargetKind {
            private final EnumSet<LibKind> myKinds;

            public Lib(EnumSet<LibKind> kinds) {
                super("lib");
                myKinds = kinds;
            }

            public Lib(LibKind... kinds) {
                this(EnumSet.copyOf(Arrays.asList(kinds)));
            }

            public EnumSet<LibKind> getKinds() { return myKinds; }

            @Override
            public boolean equals(Object o) {
                if (this == o) return true;
                if (!(o instanceof Lib)) return false;
                return myKinds.equals(((Lib) o).myKinds);
            }

            @Override
            public int hashCode() {
                return myKinds.hashCode();
            }

            @Override
            public String toString() {
                return "Lib(kinds=" + myKinds + ")";
            }
        }

        public static final class Bin extends TargetKind {
            public static final Bin INSTANCE = new Bin();
            private Bin() { super("bin"); }
            @Override public String toString() { return "Bin"; }
        }

        public static final class Test extends TargetKind {
            public static final Test INSTANCE = new Test();
            private Test() { super("test"); }
            @Override public String toString() { return "Test"; }
        }

        public static final class ExampleBin extends TargetKind {
            public static final ExampleBin INSTANCE = new ExampleBin();
            private ExampleBin() { super("example"); }
            @Override public String toString() { return "ExampleBin"; }
        }

        public static final class ExampleLib extends TargetKind {
            private final EnumSet<LibKind> myKinds;
            public ExampleLib(EnumSet<LibKind> kinds) {
                super("example");
                myKinds = kinds;
            }
            public EnumSet<LibKind> getKinds() { return myKinds; }
            @Override public String toString() { return "ExampleLib(kinds=" + myKinds + ")"; }
        }

        public static final class Bench extends TargetKind {
            public static final Bench INSTANCE = new Bench();
            private Bench() { super("bench"); }
            @Override public String toString() { return "Bench"; }
        }

        public static final class CustomBuild extends TargetKind {
            public static final CustomBuild INSTANCE = new CustomBuild();
            private CustomBuild() { super("custom-build"); }
            @Override public String toString() { return "CustomBuild"; }
        }

        public static final class Unknown extends TargetKind {
            public static final Unknown INSTANCE = new Unknown();
            private Unknown() { super("unknown"); }
            @Override public String toString() { return "Unknown"; }
        }
    }

    enum LibKind {
        LIB, DYLIB, STATICLIB, CDYLIB, RLIB, PROC_MACRO, UNKNOWN
    }

    enum Edition {
        EDITION_2015("2015"),
        EDITION_2018("2018"),
        EDITION_2021("2021");

        public static final Edition DEFAULT = EDITION_2018;

        private final String myPresentation;

        Edition(String presentation) {
            myPresentation = presentation;
        }

        public String getPresentation() {
            return myPresentation;
        }
    }

    static CargoWorkspace deserialize(
        Path manifestPath,
        CargoWorkspaceData data,
        CfgOptions cfgOptions,
        CargoConfig cargoConfig
    ) {
        return WorkspaceImpl.deserialize(manifestPath, data, cfgOptions, cargoConfig);
    }

    static CargoWorkspace deserialize(Path manifestPath, CargoWorkspaceData data) {
        return deserialize(manifestPath, data, CfgOptions.DEFAULT, CargoConfig.DEFAULT);
    }

    /**
     * A way to add additional (indexable) source roots for a package.
     * These hacks are needed for the stdlib that has a weird source structure.
     */
    @NotNull
    static List<VirtualFile> additionalRoots(@NotNull Package pkg) {
        if (pkg.getOrigin() == PackageOrigin.STDLIB) {
            String name = pkg.getName();
            if (name.equals(org.rust.cargo.util.AutoInjectedCrates.STD)) {
                VirtualFile contentRoot = pkg.getContentRoot();
                if (contentRoot != null && contentRoot.getParent() != null) {
                    VirtualFile backtrace = contentRoot.getParent().findFileByRelativePath("backtrace");
                    if (backtrace != null) {
                        return Collections.singletonList(backtrace);
                    }
                }
                return Collections.emptyList();
            } else if (name.equals(org.rust.cargo.util.AutoInjectedCrates.CORE)) {
                VirtualFile contentRoot = pkg.getContentRoot();
                if (contentRoot != null && contentRoot.getParent() != null) {
                    VirtualFile parent = contentRoot.getParent();
                    List<VirtualFile> roots = new ArrayList<>();
                    VirtualFile coreArch = parent.findFileByRelativePath("stdarch/crates/core_arch");
                    if (coreArch != null) roots.add(coreArch);
                    VirtualFile stdDetect = parent.findFileByRelativePath("stdarch/crates/std_detect");
                    if (stdDetect != null) roots.add(stdDetect);
                    VirtualFile coreSimd = parent.findFileByRelativePath("portable-simd/crates/core_simd");
                    if (coreSimd != null) roots.add(coreSimd);
                    VirtualFile stdFloat = parent.findFileByRelativePath("portable-simd/crates/std_float");
                    if (stdFloat != null) roots.add(stdFloat);
                    return roots;
                }
                return Collections.emptyList();
            }
        }
        return Collections.emptyList();
    }
}
