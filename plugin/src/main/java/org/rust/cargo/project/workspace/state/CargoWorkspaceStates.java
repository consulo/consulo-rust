/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.cargo.project.workspace.state;

import consulo.util.lang.StringUtil;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.rust.cargo.api.CfgOptions;
import org.rust.cargo.api.toolchain.CargoMetadata;
import org.rust.cargo.api.workspace.CargoWorkspace;
import org.rust.cargo.api.workspace.CargoWorkspaceData;
import org.rust.cargo.api.workspace.PackageOrigin;
import org.rust.stdext.HashCode;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Converts between {@link CargoWorkspaceData} and its bean mirror {@link CargoWorkspaceState}.
 * <p>
 * The round trip is what lets a project be opened without running Cargo: the resolved workspace goes
 * into the project model when a sync finishes, and comes back out of it when the project is reopened.
 */
public final class CargoWorkspaceStates {

    private CargoWorkspaceStates() {
    }

    @Nonnull
    public static CargoWorkspaceState toState(@Nonnull CargoWorkspaceData data) {
        return toState(data, null);
    }

    @Nonnull
    public static CargoWorkspaceState toState(@Nonnull CargoWorkspaceData data, @Nullable CfgOptions cfgOptions) {
        CargoWorkspaceState state = new CargoWorkspaceState();
        state.workspaceRootUrl = data.getWorkspaceRootUrl();
        state.hasCfgOptions = cfgOptions != null;
        if (cfgOptions != null) {
            for (Map.Entry<String, Set<String>> option : cfgOptions.keyValueOptions().entrySet()) {
                state.cfgKeyValueOptions.put(option.getKey(), new ArrayList<>(option.getValue()));
            }
            state.cfgNameOptions.addAll(cfgOptions.nameOptions());
        }

        for (CargoWorkspaceData.Package pkg : data.getPackages()) {
            state.packages.add(toState(pkg));
        }
        for (Map.Entry<String, Set<CargoWorkspaceData.Dependency>> entry : data.getDependencies().entrySet()) {
            List<CargoWorkspaceState.DependencyState> dependencies = new ArrayList<>();
            for (CargoWorkspaceData.Dependency dependency : entry.getValue()) {
                dependencies.add(toState(dependency));
            }
            state.dependencies.put(entry.getKey(), dependencies);
        }
        for (Map.Entry<String, List<CargoMetadata.RawDependency>> entry : data.getRawDependencies().entrySet()) {
            List<CargoWorkspaceState.RawDependencyState> raw = new ArrayList<>();
            for (CargoMetadata.RawDependency rawDependency : entry.getValue()) {
                raw.add(toState(rawDependency));
            }
            state.rawDependencies.put(entry.getKey(), raw);
        }
        return state;
    }

    @Nonnull
    public static CargoWorkspaceData fromState(@Nonnull CargoWorkspaceState state) {
        List<CargoWorkspaceData.Package> packages = new ArrayList<>();
        for (CargoWorkspaceState.PackageState pkg : state.packages) {
            packages.add(fromState(pkg));
        }

        Map<String, Set<CargoWorkspaceData.Dependency>> dependencies = new LinkedHashMap<>();
        for (Map.Entry<String, List<CargoWorkspaceState.DependencyState>> entry : state.dependencies.entrySet()) {
            Set<CargoWorkspaceData.Dependency> value = new LinkedHashSet<>();
            for (CargoWorkspaceState.DependencyState dependency : entry.getValue()) {
                value.add(fromState(dependency));
            }
            dependencies.put(entry.getKey(), value);
        }

        Map<String, List<CargoMetadata.RawDependency>> rawDependencies = new LinkedHashMap<>();
        for (Map.Entry<String, List<CargoWorkspaceState.RawDependencyState>> entry : state.rawDependencies.entrySet()) {
            List<CargoMetadata.RawDependency> value = new ArrayList<>();
            for (CargoWorkspaceState.RawDependencyState raw : entry.getValue()) {
                value.add(fromState(raw));
            }
            rawDependencies.put(entry.getKey(), value);
        }

        return new CargoWorkspaceData(packages, dependencies, rawDependencies, state.workspaceRootUrl);
    }

    /** The workspace-level cfg options of {@code state}, or {@code null} when none were recorded. */
    @Nullable
    public static CfgOptions cfgOptionsOf(@Nonnull CargoWorkspaceState state) {
        if (!state.hasCfgOptions) return null;
        Map<String, Set<String>> keyValueOptions = new LinkedHashMap<>();
        for (Map.Entry<String, List<String>> option : state.cfgKeyValueOptions.entrySet()) {
            keyValueOptions.put(option.getKey(), new LinkedHashSet<>(option.getValue()));
        }
        return new CfgOptions(keyValueOptions, new LinkedHashSet<>(state.cfgNameOptions));
    }

    @Nonnull
    private static CargoWorkspaceState.PackageState toState(@Nonnull CargoWorkspaceData.Package pkg) {
        CargoWorkspaceState.PackageState state = new CargoWorkspaceState.PackageState();
        state.id = pkg.getId();
        state.contentRootUrl = pkg.getContentRootUrl();
        state.name = pkg.getName();
        state.version = pkg.getVersion();
        state.source = pkg.getSource();
        state.origin = pkg.getOrigin().name();
        state.edition = pkg.getEdition().name();
        state.outDirUrl = pkg.getOutDirUrl();

        for (CargoWorkspaceData.Target target : pkg.getTargets()) {
            state.targets.add(toState(target));
        }
        for (Map.Entry<String, List<String>> feature : pkg.getFeatures().entrySet()) {
            state.features.put(feature.getKey(), new ArrayList<>(feature.getValue()));
        }
        state.enabledFeatures.addAll(pkg.getEnabledFeatures());

        CfgOptions cfgOptions = pkg.getCfgOptions();
        state.hasCfgOptions = cfgOptions != null;
        if (cfgOptions != null) {
            for (Map.Entry<String, Set<String>> option : cfgOptions.keyValueOptions().entrySet()) {
                state.cfgKeyValueOptions.put(option.getKey(), new ArrayList<>(option.getValue()));
            }
            state.cfgNameOptions.addAll(cfgOptions.nameOptions());
        }

        state.env.putAll(pkg.getEnv());

        CargoWorkspaceData.ProcMacroArtifact procMacro = pkg.getProcMacroArtifact();
        if (procMacro != null) {
            state.procMacroPath = procMacro.getPath().toString();
            state.procMacroHash = procMacro.getHash() == null ? null : procMacro.getHash().toString();
        }
        return state;
    }

    @Nonnull
    private static CargoWorkspaceData.Package fromState(@Nonnull CargoWorkspaceState.PackageState state) {
        List<CargoWorkspaceData.Target> targets = new ArrayList<>();
        for (CargoWorkspaceState.TargetState target : state.targets) {
            targets.add(fromState(target));
        }

        Map<String, List<String>> features = new LinkedHashMap<>();
        for (Map.Entry<String, List<String>> feature : state.features.entrySet()) {
            features.put(feature.getKey(), new ArrayList<>(feature.getValue()));
        }

        CfgOptions cfgOptions = null;
        if (state.hasCfgOptions) {
            Map<String, Set<String>> keyValueOptions = new LinkedHashMap<>();
            for (Map.Entry<String, List<String>> option : state.cfgKeyValueOptions.entrySet()) {
                keyValueOptions.put(option.getKey(), new LinkedHashSet<>(option.getValue()));
            }
            cfgOptions = new CfgOptions(keyValueOptions, new LinkedHashSet<>(state.cfgNameOptions));
        }

        CargoWorkspaceData.ProcMacroArtifact procMacro = null;
        if (state.procMacroPath != null) {
            procMacro = new CargoWorkspaceData.ProcMacroArtifact(Paths.get(state.procMacroPath), hash(state.procMacroHash));
        }

        return new CargoWorkspaceData.Package(
            state.id,
            state.contentRootUrl,
            state.name,
            state.version,
            targets,
            state.source,
            PackageOrigin.valueOf(state.origin),
            CargoWorkspace.Edition.valueOf(state.edition),
            features,
            new LinkedHashSet<>(state.enabledFeatures),
            cfgOptions,
            new LinkedHashMap<>(state.env),
            state.outDirUrl,
            procMacro
        );
    }

    /**
     * A hash that cannot be read back leaves the artifact without one, which is what an artifact built
     * before the hash was recorded looks like - the proc macro is then re-hashed on the next sync.
     */
    @Nullable
    private static HashCode hash(@Nullable String hex) {
        if (StringUtil.isEmpty(hex)) return null;
        try {
            return HashCode.fromHexString(hex);
        }
        catch (Exception e) {
            return null;
        }
    }

    @Nonnull
    private static CargoWorkspaceState.TargetState toState(@Nonnull CargoWorkspaceData.Target target) {
        CargoWorkspaceState.TargetState state = new CargoWorkspaceState.TargetState();
        state.crateRootUrl = target.getCrateRootUrl();
        state.name = target.getName();
        state.edition = target.getEdition().name();
        state.doctest = target.getDoctest();
        state.requiredFeatures.addAll(target.getRequiredFeatures());

        CargoWorkspace.TargetKind kind = target.getKind();
        if (kind instanceof CargoWorkspace.TargetKind.Lib lib) {
            state.kind = "lib";
            addLibKinds(state.libKinds, lib.getKinds());
        }
        else if (kind instanceof CargoWorkspace.TargetKind.ExampleLib exampleLib) {
            state.kind = "example-lib";
            addLibKinds(state.libKinds, exampleLib.getKinds());
        }
        else if (kind == CargoWorkspace.TargetKind.Bin.INSTANCE) {
            state.kind = "bin";
        }
        else if (kind == CargoWorkspace.TargetKind.Test.INSTANCE) {
            state.kind = "test";
        }
        else if (kind == CargoWorkspace.TargetKind.ExampleBin.INSTANCE) {
            state.kind = "example-bin";
        }
        else if (kind == CargoWorkspace.TargetKind.Bench.INSTANCE) {
            state.kind = "bench";
        }
        else if (kind == CargoWorkspace.TargetKind.CustomBuild.INSTANCE) {
            state.kind = "custom-build";
        }
        else {
            state.kind = "unknown";
        }
        return state;
    }

    @Nonnull
    private static CargoWorkspaceData.Target fromState(@Nonnull CargoWorkspaceState.TargetState state) {
        return new CargoWorkspaceData.Target(
            state.crateRootUrl,
            state.name,
            targetKind(state),
            CargoWorkspace.Edition.valueOf(state.edition),
            state.doctest,
            new ArrayList<>(state.requiredFeatures)
        );
    }

    @Nonnull
    private static CargoWorkspace.TargetKind targetKind(@Nonnull CargoWorkspaceState.TargetState state) {
        String kind = state.kind == null ? "unknown" : state.kind;
        return switch (kind) {
            case "lib" -> new CargoWorkspace.TargetKind.Lib(libKinds(state.libKinds));
            case "example-lib" -> new CargoWorkspace.TargetKind.ExampleLib(libKinds(state.libKinds));
            case "bin" -> CargoWorkspace.TargetKind.Bin.INSTANCE;
            case "test" -> CargoWorkspace.TargetKind.Test.INSTANCE;
            case "example-bin" -> CargoWorkspace.TargetKind.ExampleBin.INSTANCE;
            case "bench" -> CargoWorkspace.TargetKind.Bench.INSTANCE;
            case "custom-build" -> CargoWorkspace.TargetKind.CustomBuild.INSTANCE;
            default -> CargoWorkspace.TargetKind.Unknown.INSTANCE;
        };
    }

    private static void addLibKinds(@Nonnull List<String> target, @Nullable Collection<CargoWorkspace.LibKind> kinds) {
        if (kinds == null) return;
        for (CargoWorkspace.LibKind kind : kinds) {
            target.add(kind.name());
        }
    }

    @Nonnull
    private static EnumSet<CargoWorkspace.LibKind> libKinds(@Nonnull List<String> names) {
        EnumSet<CargoWorkspace.LibKind> kinds = EnumSet.noneOf(CargoWorkspace.LibKind.class);
        for (String name : names) {
            try {
                kinds.add(CargoWorkspace.LibKind.valueOf(name));
            }
            catch (IllegalArgumentException ignored) {
                // a library kind this build does not know; the target keeps the kinds it can represent
            }
        }
        if (kinds.isEmpty()) {
            kinds.add(CargoWorkspace.LibKind.LIB);
        }
        return kinds;
    }

    @Nonnull
    private static CargoWorkspaceState.DependencyState toState(@Nonnull CargoWorkspaceData.Dependency dependency) {
        CargoWorkspaceState.DependencyState state = new CargoWorkspaceState.DependencyState();
        state.id = dependency.getId();
        state.name = dependency.getName();
        for (CargoWorkspace.DepKindInfo depKind : dependency.getDepKinds()) {
            CargoWorkspaceState.DepKindState kindState = new CargoWorkspaceState.DepKindState();
            kindState.kind = depKind.getKind().name();
            kindState.target = depKind.getTarget();
            state.depKinds.add(kindState);
        }
        return state;
    }

    @Nonnull
    private static CargoWorkspaceData.Dependency fromState(@Nonnull CargoWorkspaceState.DependencyState state) {
        List<CargoWorkspace.DepKindInfo> depKinds = new ArrayList<>();
        for (CargoWorkspaceState.DepKindState kindState : state.depKinds) {
            CargoWorkspace.DepKind kind;
            try {
                kind = CargoWorkspace.DepKind.valueOf(kindState.kind);
            }
            catch (IllegalArgumentException | NullPointerException e) {
                kind = CargoWorkspace.DepKind.Unclassified;
            }
            depKinds.add(new CargoWorkspace.DepKindInfo(kind, kindState.target));
        }
        return new CargoWorkspaceData.Dependency(state.id, state.name, depKinds);
    }

    @Nonnull
    private static CargoWorkspaceState.RawDependencyState toState(@Nonnull CargoMetadata.RawDependency raw) {
        CargoWorkspaceState.RawDependencyState state = new CargoWorkspaceState.RawDependencyState();
        state.name = raw.getName();
        state.rename = raw.getRename();
        state.kind = raw.getKind();
        state.target = raw.getTarget();
        state.optional = raw.isOptional();
        state.usesDefaultFeatures = raw.isUses_default_features();
        if (raw.getFeatures() != null) {
            state.features.addAll(raw.getFeatures());
        }
        return state;
    }

    @Nonnull
    private static CargoMetadata.RawDependency fromState(@Nonnull CargoWorkspaceState.RawDependencyState state) {
        return new CargoMetadata.RawDependency(
            state.name,
            state.rename,
            state.kind,
            state.target,
            state.optional,
            state.usesDefaultFeatures,
            new ArrayList<>(state.features)
        );
    }
}
