/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.crate.impl;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.SimpleModificationTracker;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.VirtualFileManager;
import com.intellij.openapi.vfs.VirtualFileWithId;
import com.intellij.psi.util.CachedValue;
import com.intellij.psi.util.CachedValueProvider;
import com.intellij.util.CachedValueImpl;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.TestOnly;
import org.rust.cargo.project.model.CargoProject;
import org.rust.cargo.project.model.CargoProjectsService;
import org.rust.cargo.project.model.CargoProjectServiceUtil;
import org.rust.cargo.project.workspace.CargoWorkspace;
import org.rust.cargo.project.workspace.FeatureState;
import org.rust.cargo.project.workspace.PackageOrigin;
import org.rust.lang.core.crate.Crate;
import org.rust.lang.core.crate.CrateGraphService;
import org.rust.openapiext.OpenApiUtil;
import org.rust.stdext.StdextUtil;

import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;

public class CrateGraphServiceImpl implements CrateGraphService {

    @NotNull
    private final Project project;
    @NotNull
    private final SimpleModificationTracker cargoProjectsModTracker = new SimpleModificationTracker();
    @NotNull
    private final CachedValue<CrateGraph> crateGraphCachedValue;

    public CrateGraphServiceImpl(@NotNull Project project) {
        this.project = project;
        project.getMessageBus().connect().subscribe(
            CargoProjectsService.CARGO_PROJECTS_TOPIC,
            (CargoProjectsService.CargoProjectsListener) (projects, updated) -> cargoProjectsModTracker.incModificationCount()
        );

        this.crateGraphCachedValue = new CachedValueImpl<>(() -> {
            CrateGraph result = buildCrateGraph(CargoProjectServiceUtil.getCargoProjects(project).getAllProjects());
            return CachedValueProvider.Result.create(result, cargoProjectsModTracker, VirtualFileManager.VFS_STRUCTURE_MODIFICATIONS);
        });
    }

    @NotNull
    private CrateGraph getCrateGraph() {
        return crateGraphCachedValue.getValue();
    }

    @NotNull
    @Override
    public List<Crate> getTopSortedCrates() {
        org.rust.openapiext.OpenApiUtil.checkReadAccessAllowed();
        return getCrateGraph().topSortedCrates;
    }

    @Nullable
    @Override
    public Crate findCrateById(int id) {
        org.rust.openapiext.OpenApiUtil.checkReadAccessAllowed();
        return getCrateGraph().idToCrate.get(id);
    }

    @Nullable
    @Override
    public Crate findCrateByRootMod(@NotNull VirtualFile rootModFile) {
        org.rust.openapiext.OpenApiUtil.checkReadAccessAllowed();
        return StdextUtil.applyWithSymlink(rootModFile, vf -> {
            if (vf instanceof VirtualFileWithId) {
                return findCrateById(((VirtualFileWithId) vf).getId());
            }
            return null;
        });
    }

    @TestOnly
    public boolean hasUpToDateGraph() {
        return crateGraphCachedValue.hasUpToDateValue();
    }

    // ---- Inner classes ----

    private static class CrateGraph {
        @NotNull
        final List<Crate> topSortedCrates;
        @NotNull
        final Int2ObjectMap<Crate> idToCrate;

        CrateGraph(@NotNull List<Crate> topSortedCrates, @NotNull Int2ObjectMap<Crate> idToCrate) {
            this.topSortedCrates = topSortedCrates;
            this.idToCrate = idToCrate;
        }
    }

    private static final Logger LOG = Logger.getInstance(CrateGraphServiceImpl.class);

    @NotNull
    private static CrateGraph buildCrateGraph(@NotNull Collection<CargoProject> cargoProjects) {
        CrateGraphBuilder builder = new CrateGraphBuilder();
        for (CargoProject cargoProject : cargoProjects) {
            CargoWorkspace workspace = cargoProject.getWorkspace();
            if (workspace == null) continue;
            for (CargoWorkspace.Package pkg : workspace.getPackages()) {
                try {
                    builder.lowerPackage(new ProjectPackage(cargoProject, pkg, pkg.getRootDirectory()));
                } catch (CyclicGraphException e) {
                    LOG.error(e);
                }
            }
        }
        return builder.build();
    }

    // ---- CrateGraphBuilder ----

    private static class CrateGraphBuilder {
        private final Map<Path, NodeState> states = new HashMap<>();
        private final List<CargoBasedCrate> topSortedCrates = new ArrayList<>();
        private final List<NonLibraryCrates> cratesToLowerLater = new ArrayList<>();
        private final List<ReplaceProjectAndTarget> cratesToReplaceTargetLater = new ArrayList<>();

        @Nullable
        CargoBasedCrate lowerPackage(@NotNull ProjectPackage pkg) {
            NodeState state = states.get(pkg.rootDirectory);
            if (state instanceof NodeState.Done) {
                NodeState.Done done = (NodeState.Done) state;
                CargoBasedCrate libCrate = done.libCrate;
                if (done.pkgs.add(pkg.pkg)) {
                    if (libCrate != null) {
                        libCrate.setFeatures(mergeFeatures(pkg.pkg.getFeatureState(), libCrate.getFeatures()));
                    }
                    if (pkg.pkg.getOrigin() == PackageOrigin.WORKSPACE) {
                        cratesToReplaceTargetLater.add(new ReplaceProjectAndTarget(done, pkg));
                    }
                    if (pkg.pkg.getProcMacroArtifact() != null && done.libCrate != null) {
                        done.libCrate.setProcMacroArtifact(pkg.pkg.getProcMacroArtifact());
                    }
                }
                return libCrate;
            } else if (state instanceof NodeState.Processing) {
                throw new CyclicGraphException(pkg.pkg.getName());
            } else {
                states.put(pkg.rootDirectory, NodeState.Processing.INSTANCE);
            }

            LoweredPackageDependencies lowered = lowerPackageDependencies(pkg);

            CargoWorkspace.Target customBuildTarget = pkg.pkg.getCustomBuildTarget();
            CargoBasedCrate customBuildCrate = null;
            if (customBuildTarget != null) {
                customBuildCrate = new CargoBasedCrate(pkg.project, customBuildTarget, lowered.buildDeps,
                    Util.flattenTopSortedDeps(lowered.buildDeps));
            }
            if (customBuildCrate != null) {
                topSortedCrates.add(customBuildCrate);
            }

            LinkedHashSet<Crate> flatNormalAndNonCyclicDevDeps = Util.flattenTopSortedDeps(lowered.normalAndNonCyclicDevDeps);
            CargoWorkspace.Target libTarget = pkg.pkg.getLibTarget();
            CargoBasedCrate libCrate = null;
            if (libTarget != null) {
                libCrate = new CargoBasedCrate(
                    pkg.project,
                    libTarget,
                    lowered.normalAndNonCyclicDevDeps,
                    flatNormalAndNonCyclicDevDeps,
                    pkg.pkg.getProcMacroArtifact()
                );
            }

            NodeState.Done newState = new NodeState.Done(libCrate);
            newState.pkgs.add(pkg.pkg);
            if (customBuildCrate != null) {
                newState.nonLibraryCrates.add(customBuildCrate);
            }

            states.put(pkg.rootDirectory, newState);
            if (libCrate != null) {
                topSortedCrates.add(libCrate);
            }

            NonLibraryCrates nlc = new NonLibraryCrates(
                pkg, newState, lowered.normalAndNonCyclicDevDeps,
                lowered.cyclicDevDependencies, flatNormalAndNonCyclicDevDeps
            );
            lowerNonLibraryCratesLater(nlc);

            return libCrate;
        }

        private static class ReplaceProjectAndTarget {
            final NodeState.Done state;
            final ProjectPackage pkg;

            ReplaceProjectAndTarget(NodeState.Done state, ProjectPackage pkg) {
                this.state = state;
                this.pkg = pkg;
            }
        }

        private void replaceProjectAndTarget(@NotNull ReplaceProjectAndTarget ctx) {
            CargoBasedCrate libCrate = ctx.state.libCrate;
            if (libCrate != null) {
                CargoWorkspace.Target lt = ctx.pkg.pkg.getLibTarget();
                if (lt != null) {
                    libCrate.setCargoTarget(lt);
                    libCrate.setCargoProject(ctx.pkg.project);
                }
            }
            for (CargoBasedCrate crate : ctx.state.nonLibraryCrates) {
                CargoWorkspace.Target newTarget = null;
                for (CargoWorkspace.Target t : ctx.pkg.pkg.getTargets()) {
                    if (t.getName().equals(crate.getCargoTarget().getName())) {
                        newTarget = t;
                        break;
                    }
                }
                if (newTarget != null) {
                    crate.setCargoTarget(newTarget);
                    crate.setCargoProject(ctx.pkg.project);
                }
            }
        }

        private static class LoweredPackageDependencies {
            final List<Crate.Dependency> buildDeps;
            final List<Crate.Dependency> normalAndNonCyclicDevDeps;
            final List<CargoWorkspace.Dependency> cyclicDevDependencies;

            LoweredPackageDependencies(
                List<Crate.Dependency> buildDeps,
                List<Crate.Dependency> normalAndNonCyclicDevDeps,
                List<CargoWorkspace.Dependency> cyclicDevDependencies
            ) {
                this.buildDeps = buildDeps;
                this.normalAndNonCyclicDevDeps = normalAndNonCyclicDevDeps;
                this.cyclicDevDependencies = cyclicDevDependencies;
            }
        }

        @NotNull
        private LoweredPackageDependencies lowerPackageDependencies(@NotNull ProjectPackage pkg) {
            SplitDependencies classified = classify(pkg.pkg.getDependencies());
            if (classified instanceof SplitDependencies.Classified) {
                SplitDependencies.Classified c = (SplitDependencies.Classified) classified;
                List<Crate.Dependency> buildDeps = lowerDependencies(c.build, pkg);
                List<Crate.Dependency> normalDeps = lowerDependencies(c.normal, pkg);
                LoweredAndCyclicDependencies devResult = lowerDependenciesWithCycles(c.dev, pkg);
                List<Crate.Dependency> normalAndNonCyclicDevDeps = new ArrayList<>(normalDeps);
                normalAndNonCyclicDevDeps.addAll(devResult.lowered);
                return new LoweredPackageDependencies(buildDeps, normalAndNonCyclicDevDeps, devResult.cyclic);
            } else {
                SplitDependencies.Unclassified u = (SplitDependencies.Unclassified) classified;
                LoweredAndCyclicDependencies result = lowerDependenciesWithCycles(u.dependencies, pkg);
                return new LoweredPackageDependencies(result.lowered, result.lowered, result.cyclic);
            }
        }

        private static abstract class SplitDependencies {
            static class Classified extends SplitDependencies {
                final Collection<CargoWorkspace.Dependency> normal;
                final Collection<CargoWorkspace.Dependency> dev;
                final Collection<CargoWorkspace.Dependency> build;

                Classified(Collection<CargoWorkspace.Dependency> normal,
                           Collection<CargoWorkspace.Dependency> dev,
                           Collection<CargoWorkspace.Dependency> build) {
                    this.normal = normal;
                    this.dev = dev;
                    this.build = build;
                }
            }

            static class Unclassified extends SplitDependencies {
                final Collection<CargoWorkspace.Dependency> dependencies;

                Unclassified(Collection<CargoWorkspace.Dependency> dependencies) {
                    this.dependencies = dependencies;
                }
            }
        }

        @NotNull
        private static SplitDependencies classify(@NotNull Collection<CargoWorkspace.Dependency> dependencies) {
            List<CargoWorkspace.Dependency> unclassified = new ArrayList<>();
            List<CargoWorkspace.Dependency> normal = new ArrayList<>();
            Set<CargoWorkspace.Dependency> dev = new LinkedHashSet<>();
            List<CargoWorkspace.Dependency> build = new ArrayList<>();

            for (CargoWorkspace.Dependency dependency : dependencies) {
                EnumSet<CargoWorkspace.DepKind> visitedDepKinds = EnumSet.noneOf(CargoWorkspace.DepKind.class);

                for (CargoWorkspace.DepKindInfo depKind : dependency.getDepKinds()) {
                    if (!visitedDepKinds.add(depKind.getKind())) continue;

                    switch (depKind.getKind()) {
                        case Stdlib:
                            normal.add(dependency);
                            build.add(dependency);
                            break;
                        case Unclassified:
                            unclassified.add(dependency);
                            break;
                        case Normal:
                            normal.add(dependency);
                            break;
                        case Development:
                            dev.add(dependency);
                            break;
                        case Build:
                            build.add(dependency);
                            break;
                    }
                }
            }

            dev.removeAll(normal);

            if (!unclassified.isEmpty()) {
                List<CargoWorkspace.Dependency> all = new ArrayList<>(unclassified);
                all.addAll(normal);
                return new SplitDependencies.Unclassified(all);
            } else {
                return new SplitDependencies.Classified(normal, dev, build);
            }
        }

        @NotNull
        private List<Crate.Dependency> lowerDependencies(
            @NotNull Iterable<CargoWorkspace.Dependency> deps,
            @NotNull ProjectPackage pkg
        ) {
            try {
                List<Crate.Dependency> result = new ArrayList<>();
                for (CargoWorkspace.Dependency dep : deps) {
                    CargoBasedCrate crate = lowerPackage(new ProjectPackage(pkg.project, dep.getPkg(), dep.getPkg().getRootDirectory()));
                    if (crate != null) {
                        result.add(new Crate.Dependency(dep.getName(), crate));
                    }
                }
                return result;
            } catch (CyclicGraphException e) {
                states.remove(pkg.rootDirectory);
                e.pushCrate(pkg.pkg.getName());
                throw e;
            }
        }

        private static class LoweredAndCyclicDependencies {
            final List<Crate.Dependency> lowered;
            final List<CargoWorkspace.Dependency> cyclic;

            LoweredAndCyclicDependencies(List<Crate.Dependency> lowered, List<CargoWorkspace.Dependency> cyclic) {
                this.lowered = lowered;
                this.cyclic = cyclic;
            }
        }

        @NotNull
        private LoweredAndCyclicDependencies lowerDependenciesWithCycles(
            @NotNull Collection<CargoWorkspace.Dependency> devDependencies,
            @NotNull ProjectPackage pkg
        ) {
            List<CargoWorkspace.Dependency> cyclic = new ArrayList<>();
            List<Crate.Dependency> lowered = new ArrayList<>();
            for (CargoWorkspace.Dependency dep : devDependencies) {
                try {
                    CargoBasedCrate crate = lowerPackage(new ProjectPackage(pkg.project, dep.getPkg(), dep.getPkg().getRootDirectory()));
                    if (crate != null) {
                        lowered.add(new Crate.Dependency(dep.getName(), crate));
                    }
                } catch (CyclicGraphException ignored) {
                    Util.CrateGraphTestmarks.INSTANCE.hit();
                    cyclic.add(dep);
                }
            }
            return new LoweredAndCyclicDependencies(lowered, cyclic);
        }

        private void lowerNonLibraryCratesLater(@NotNull NonLibraryCrates ctx) {
            if (ctx.cyclicDevDependencies.isEmpty()) {
                lowerNonLibraryCrates(ctx);
            } else {
                cratesToLowerLater.add(ctx);
            }
        }

        private static class NonLibraryCrates {
            final ProjectPackage pkg;
            final NodeState.Done doneState;
            final List<Crate.Dependency> normalAndNonCyclicTestDeps;
            final List<CargoWorkspace.Dependency> cyclicDevDependencies;
            final LinkedHashSet<Crate> flatNormalAndNonCyclicDevDeps;

            NonLibraryCrates(ProjectPackage pkg, NodeState.Done doneState,
                             List<Crate.Dependency> normalAndNonCyclicTestDeps,
                             List<CargoWorkspace.Dependency> cyclicDevDependencies,
                             LinkedHashSet<Crate> flatNormalAndNonCyclicDevDeps) {
                this.pkg = pkg;
                this.doneState = doneState;
                this.normalAndNonCyclicTestDeps = normalAndNonCyclicTestDeps;
                this.cyclicDevDependencies = cyclicDevDependencies;
                this.flatNormalAndNonCyclicDevDeps = flatNormalAndNonCyclicDevDeps;
            }
        }

        private void lowerNonLibraryCrates(@NotNull NonLibraryCrates ctx) {
            List<Crate.Dependency> cyclicDevDeps = lowerDependencies(ctx.cyclicDevDependencies, ctx.pkg);
            List<Crate.Dependency> normalAndTestDeps = new ArrayList<>(ctx.normalAndNonCyclicTestDeps);
            normalAndTestDeps.addAll(cyclicDevDeps);

            CargoBasedCrate libCrate = ctx.doneState.libCrate;
            List<Crate.Dependency> depsWithLib;
            LinkedHashSet<Crate> flatDepsWithLib;
            if (libCrate != null) {
                Crate.Dependency libDep = new Crate.Dependency(libCrate.getNormName(), libCrate);

                flatDepsWithLib = new LinkedHashSet<>(ctx.flatNormalAndNonCyclicDevDeps);
                flatDepsWithLib.addAll(Util.flattenTopSortedDeps(cyclicDevDeps));
                flatDepsWithLib.add(libCrate);

                depsWithLib = new ArrayList<>(normalAndTestDeps);
                depsWithLib.add(libDep);
            } else {
                depsWithLib = normalAndTestDeps;
                flatDepsWithLib = ctx.flatNormalAndNonCyclicDevDeps;
            }

            List<CargoBasedCrate> nonLibraryCrates = new ArrayList<>();
            for (CargoWorkspace.Target target : ctx.pkg.pkg.getTargets()) {
                if (target.getKind().isLib() || target.getKind() == CargoWorkspace.TargetKind.CustomBuild.INSTANCE) continue;
                nonLibraryCrates.add(new CargoBasedCrate(ctx.pkg.project, target, depsWithLib, flatDepsWithLib));
            }

            ctx.doneState.nonLibraryCrates.addAll(nonLibraryCrates);
            topSortedCrates.addAll(nonLibraryCrates);
            if (!cyclicDevDeps.isEmpty() && libCrate != null) {
                libCrate.setCyclicDevDeps(cyclicDevDeps);
            }
        }

        @NotNull
        CrateGraph build() {
            for (NonLibraryCrates ctx : cratesToLowerLater) {
                lowerNonLibraryCrates(ctx);
            }
            for (ReplaceProjectAndTarget ctx : cratesToReplaceTargetLater) {
                replaceProjectAndTarget(ctx);
            }

            assertTopSorted(topSortedCrates);

            Int2ObjectOpenHashMap<Crate> idToCrate = new Int2ObjectOpenHashMap<>();
            for (CargoBasedCrate crate : topSortedCrates) {
                checkInvariants(crate);
                Integer id = crate.getId();
                if (id != null) {
                    idToCrate.put((int) id, crate);
                }
            }
            return new CrateGraph(new ArrayList<>(topSortedCrates), idToCrate);
        }
    }

    private static void checkInvariants(@NotNull Crate crate) {
        if (!org.rust.openapiext.OpenApiUtil.isUnitTestMode()) return;

        assertTopSorted(crate.getFlatDependencies());

        for (Crate.Dependency dep : crate.getDependencies()) {
            if (!crate.getFlatDependencies().contains(dep.getCrate())) {
                throw new IllegalStateException(
                    "Error in structure of crate `" + crate + "`: no `" + dep.getCrate() +
                        "` dependency in flatDependencies: " + crate.getFlatDependencies()
                );
            }
        }
    }

    private static void assertTopSorted(@NotNull Iterable<? extends Crate> crates) {
        if (!org.rust.openapiext.OpenApiUtil.isUnitTestMode()) return;
        Set<Crate> set = new HashSet<>();
        for (Crate crate : crates) {
            for (Crate.Dependency dep : crate.getDependencies()) {
                if (!set.contains(dep.getCrate())) {
                    throw new IllegalStateException("Crates are not topologically sorted");
                }
            }
            set.add(crate);
        }
    }

    @NotNull
    private static Map<String, FeatureState> mergeFeatures(
        @NotNull Map<String, FeatureState> features1,
        @NotNull Map<String, FeatureState> features2
    ) {
        Map<String, FeatureState> featureMap = new LinkedHashMap<>(features1);
        for (Map.Entry<String, FeatureState> entry : features2.entrySet()) {
            featureMap.merge(entry.getKey(), entry.getValue(), (v1, v2) -> {
                if (v1 == FeatureState.Enabled) return FeatureState.Enabled;
                if (v2 == FeatureState.Enabled) return FeatureState.Enabled;
                return FeatureState.Disabled;
            });
        }
        return featureMap;
    }

    private static class ProjectPackage {
        @NotNull final CargoProject project;
        @NotNull final CargoWorkspace.Package pkg;
        @NotNull final Path rootDirectory;

        ProjectPackage(@NotNull CargoProject project, @NotNull CargoWorkspace.Package pkg, @NotNull Path rootDirectory) {
            this.project = project;
            this.pkg = pkg;
            this.rootDirectory = rootDirectory;
        }
    }

    private static abstract class NodeState {
        static class Done extends NodeState {
            @Nullable
            final CargoBasedCrate libCrate;
            @NotNull
            final List<CargoBasedCrate> nonLibraryCrates = new ArrayList<>();
            @NotNull
            final Set<CargoWorkspace.Package> pkgs = new HashSet<>();

            Done(@Nullable CargoBasedCrate libCrate) {
                this.libCrate = libCrate;
            }
        }

        static class Processing extends NodeState {
            static final Processing INSTANCE = new Processing();
        }
    }

    private static class CyclicGraphException extends RuntimeException {
        private final List<String> stack;

        CyclicGraphException(@NotNull String crateName) {
            super("Cyclic graph detected");
            this.stack = new ArrayList<>();
            this.stack.add(crateName);
        }

        void pushCrate(@NotNull String crateName) {
            stack.add(crateName);
        }

        @Override
        public String getMessage() {
            List<String> reversed = new ArrayList<>(stack);
            Collections.reverse(reversed);
            return super.getMessage() + " (" + String.join(" -> ", reversed) + ")";
        }
    }
}
