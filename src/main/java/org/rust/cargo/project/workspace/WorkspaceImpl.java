/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.cargo.project.workspace;

import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.VirtualFileManager;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.TestOnly;
import org.rust.cargo.CargoConfig;
import org.rust.cargo.CfgOptions;
import org.rust.cargo.project.model.RustcInfo;
import org.rust.cargo.project.model.impl.UserDisabledFeatures;
import org.rust.openapiext.CachedVirtualFile;

import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;

final class WorkspaceImpl implements CargoWorkspace {

    private final Path myManifestPath;
    private final String myWorkspaceRootUrl;
    private final List<PackageImpl> myPackages;
    private final CfgOptions myCfgOptions;
    private final CargoConfig myCargoConfig;
    private final Map<Path, Map<String, FeatureState>> myFeaturesState;

    private final CachedVirtualFile myWorkspaceRootCache;
    private volatile FeatureGraph myCachedFeatureGraph;
    private final Map<String, TargetImpl> myTargetByCrateRootUrl;

    WorkspaceImpl(
        Path manifestPath,
        @Nullable String workspaceRootUrl,
        Collection<CargoWorkspaceData.Package> packagesData,
        CfgOptions cfgOptions,
        CargoConfig cargoConfig,
        Map<Path, Map<String, FeatureState>> featuresState
    ) {
        myManifestPath = manifestPath;
        myWorkspaceRootUrl = workspaceRootUrl;
        myCfgOptions = cfgOptions;
        myCargoConfig = cargoConfig;
        myFeaturesState = featuresState;
        myWorkspaceRootCache = new CachedVirtualFile(workspaceRootUrl);

        List<PackageImpl> packages = new ArrayList<>();
        for (CargoWorkspaceData.Package pkg : packagesData) {
            packages.add(new PackageImpl(
                this,
                pkg.getId(),
                pkg.getContentRootUrl(),
                pkg.getName(),
                pkg.getVersion(),
                pkg.getTargets(),
                pkg.getSource(),
                pkg.getOrigin(),
                pkg.getEdition(),
                pkg.getCfgOptions(),
                pkg.getFeatures(),
                pkg.getEnabledFeatures(),
                pkg.getEnv(),
                pkg.getOutDirUrl(),
                pkg.getProcMacroArtifact()
            ));
        }
        myPackages = packages;

        Map<String, TargetImpl> targetMap = new HashMap<>();
        for (PackageImpl pkg : myPackages) {
            for (TargetImpl target : pkg.getTargetsInternal()) {
                targetMap.put(target.getCrateRootUrl(), target);
            }
        }
        myTargetByCrateRootUrl = targetMap;
    }

    @Override
    public Path getManifestPath() {
        return myManifestPath;
    }

    @Override
    @Nullable
    public VirtualFile getWorkspaceRoot() {
        return myWorkspaceRootCache.getValue();
    }

    String getWorkspaceRootUrl() {
        return myWorkspaceRootUrl;
    }

    @Override
    public CfgOptions getCfgOptions() {
        return myCfgOptions;
    }

    @Override
    public CargoConfig getCargoConfig() {
        return myCargoConfig;
    }

    @Override
    @SuppressWarnings("unchecked")
    public Collection<CargoWorkspace.Package> getPackages() {
        return (Collection<CargoWorkspace.Package>) (Collection<?>) myPackages;
    }

    List<PackageImpl> getPackagesInternal() {
        return myPackages;
    }

    Map<Path, Map<String, FeatureState>> getFeaturesState() {
        return myFeaturesState;
    }

    @Override
    public FeatureGraph getFeatureGraph() {
        FeatureGraph cached = myCachedFeatureGraph;
        if (cached != null) return cached;
        synchronized (this) {
            cached = myCachedFeatureGraph;
            if (cached != null) return cached;
            cached = buildFeatureGraph();
            myCachedFeatureGraph = cached;
            return cached;
        }
    }

    private FeatureGraph buildFeatureGraph() {
        Map<PackageFeature, List<PackageFeature>> wrappedFeatures = new HashMap<>();

        for (PackageImpl pkg : myPackages) {
            Map<String, List<String>> pkgFeatures = pkg.getRawFeatures();
            for (PackageFeature packageFeature : pkg.getFeatures()) {
                if (wrappedFeatures.containsKey(packageFeature)) continue;
                List<String> deps = pkgFeatures.get(packageFeature.getName());
                if (deps == null) continue;

                List<PackageFeature> wrappedDeps = new ArrayList<>();
                for (String featureDep : deps) {
                    if (featureDep.startsWith("dep:")) {
                        // skip
                    } else if (pkgFeatures.containsKey(featureDep)) {
                        wrappedDeps.add(new PackageFeature(pkg, featureDep));
                    } else if (featureDep.contains("/")) {
                        String[] parts = featureDep.split("/", 2);
                        String firstSegment = parts[0];
                        String name = parts[1];
                        boolean optional = firstSegment.endsWith("?");
                        String depName = optional ? firstSegment.substring(0, firstSegment.length() - 1) : firstSegment;

                        DependencyImpl dep = null;
                        for (DependencyImpl d : pkg.getDependenciesInternal()) {
                            if (d.getCargoFeatureDependencyPackageName().equals(depName)) {
                                dep = d;
                                break;
                            }
                        }
                        if (dep == null) continue;

                        if (dep.getPkgImpl().getRawFeatures().containsKey(name)) {
                            if (!optional && dep.isOptional()) {
                                wrappedDeps.add(new PackageFeature(pkg, dep.getCargoFeatureDependencyPackageName()));
                                wrappedDeps.add(new PackageFeature(dep.getPkgImpl(), name));
                            } else {
                                wrappedDeps.add(new PackageFeature(dep.getPkgImpl(), name));
                            }
                        }
                    }
                    // else skip
                }
                wrappedFeatures.put(packageFeature, wrappedDeps);
            }
        }
        return FeatureGraph.buildFor(wrappedFeatures);
    }

    @Override
    @Nullable
    public CargoWorkspace.Target findTargetByCrateRoot(VirtualFile root) {
        TargetImpl result = myTargetByCrateRootUrl.get(root.getUrl());
        if (result != null) return result;
        VirtualFile canonical = root.getCanonicalFile();
        if (canonical != null && canonical != root) {
            return myTargetByCrateRootUrl.get(canonical.getUrl());
        }
        return null;
    }

    @Override
    public CargoWorkspace withStdlib(StandardLibrary stdlib, CfgOptions cfgOptions, @Nullable RustcInfo rustcInfo) {
        List<CargoWorkspaceData.Package> newPackagesData;
        StandardLibrary effectiveStdlib;

        if (!stdlib.isPartOfCargoProject()) {
            newPackagesData = new ArrayList<>();
            for (PackageImpl pkg : myPackages) {
                newPackagesData.add(pkg.asPackageData(null));
            }
            newPackagesData.addAll(StandardLibraryHelper.asPackageData(stdlib, rustcInfo));
            effectiveStdlib = stdlib;
        } else {
            List<CargoWorkspaceData.Package> oldPackagesData = new ArrayList<>();
            for (PackageImpl pkg : myPackages) {
                oldPackagesData.add(pkg.asPackageData(null));
            }
            Set<String> stdCratePackageRoots = new HashSet<>();
            for (CargoWorkspaceData.Package crate : stdlib.getCrates()) {
                stdCratePackageRoots.add(crate.getContentRootUrl());
            }
            List<CargoWorkspaceData.Package> stdPackagesData = new ArrayList<>();
            List<CargoWorkspaceData.Package> otherPackagesData = new ArrayList<>();
            for (CargoWorkspaceData.Package pkg : oldPackagesData) {
                if (stdCratePackageRoots.contains(pkg.getContentRootUrl())) {
                    stdPackagesData.add(pkg);
                } else {
                    otherPackagesData.add(pkg);
                }
            }
            Map<String, CargoWorkspaceData.Package> stdPackagesByPackageRoot = new HashMap<>();
            for (CargoWorkspaceData.Package pkg : stdPackagesData) {
                stdPackagesByPackageRoot.put(pkg.getContentRootUrl(), pkg);
            }
            Map<String, String> pkgIdMapping = new HashMap<>();
            for (CargoWorkspaceData.Package crate : stdlib.getCrates()) {
                CargoWorkspaceData.Package mapped = stdPackagesByPackageRoot.get(crate.getContentRootUrl());
                pkgIdMapping.put(crate.getId(), mapped != null ? mapped.getId() : crate.getId());
            }
            List<CargoWorkspaceData.Package> newStdlibCrates = new ArrayList<>();
            for (CargoWorkspaceData.Package crate : stdlib.getCrates()) {
                newStdlibCrates.add(crate.copyWithId(pkgIdMapping.get(crate.getId())));
            }
            Map<String, Set<CargoWorkspaceData.Dependency>> newStdlibDependencies = new HashMap<>();
            for (Map.Entry<String, Set<CargoWorkspaceData.Dependency>> entry : stdlib.getWorkspaceData().getDependencies().entrySet()) {
                String oldId = entry.getKey();
                Set<CargoWorkspaceData.Dependency> newDeps = new HashSet<>();
                for (CargoWorkspaceData.Dependency dep : entry.getValue()) {
                    newDeps.add(dep.copyWithId(pkgIdMapping.get(dep.getId())));
                }
                newStdlibDependencies.put(pkgIdMapping.get(oldId), newDeps);
            }

            newPackagesData = new ArrayList<>(otherPackagesData);
            for (CargoWorkspaceData.Package pkg : stdPackagesData) {
                newPackagesData.add(pkg.copyWithOrigin(PackageOrigin.STDLIB));
            }

            CargoWorkspaceData newWorkspaceData = stdlib.getWorkspaceData().copy(
                newStdlibCrates, newStdlibDependencies,
                stdlib.getWorkspaceData().getRawDependencies(),
                stdlib.getWorkspaceData().getWorkspaceRootUrl()
            );
            effectiveStdlib = new StandardLibrary(newWorkspaceData, stdlib.isHardcoded(), true);
        }

        Map<String, CargoWorkspaceData.Package> stdAll = new HashMap<>();
        for (CargoWorkspaceData.Package crate : effectiveStdlib.getCrates()) {
            stdAll.put(crate.getId(), crate);
        }
        Set<String> stdInternalDeps = new HashSet<>();
        for (CargoWorkspaceData.Package crate : effectiveStdlib.getCrates()) {
            if (crate.getOrigin() == PackageOrigin.STDLIB_DEPENDENCY) {
                stdInternalDeps.add(crate.getId());
            }
        }

        WorkspaceImpl result = new WorkspaceImpl(
            myManifestPath,
            myWorkspaceRootUrl,
            newPackagesData,
            cfgOptions,
            myCargoConfig,
            myFeaturesState
        );

        Map<String, PackageImpl> oldIdToPackage = new HashMap<>();
        for (PackageImpl pkg : myPackages) {
            oldIdToPackage.put(pkg.getId(), pkg);
        }
        Map<String, PackageImpl> newIdToPackage = new HashMap<>();
        for (PackageImpl pkg : result.myPackages) {
            newIdToPackage.put(pkg.getId(), pkg);
        }
        List<DependencyImpl> stdlibDependencies = new ArrayList<>();
        for (PackageImpl pkg : result.myPackages) {
            if (pkg.getOrigin() == PackageOrigin.STDLIB) {
                stdlibDependencies.add(new DependencyImpl(
                    pkg,
                    Collections.singletonList(new CargoWorkspace.DepKindInfo(CargoWorkspace.DepKind.Stdlib))
                ));
            }
        }

        for (Map.Entry<String, PackageImpl> entry : newIdToPackage.entrySet()) {
            String id = entry.getKey();
            PackageImpl pkg = entry.getValue();
            CargoWorkspaceData.Package stdCrate = stdAll.get(id);
            if (stdCrate == null) {
                PackageImpl oldPkg = oldIdToPackage.get(id);
                if (oldPkg != null) {
                    for (DependencyImpl dep : oldPkg.getDependenciesInternal()) {
                        PackageImpl dependencyPackage = newIdToPackage.get(dep.getPkgImpl().getId());
                        if (dependencyPackage != null) {
                            pkg.getDependenciesInternal().add(dep.withPackage(dependencyPackage));
                        }
                    }
                }
                Set<String> explicitDeps = new HashSet<>();
                for (DependencyImpl dep : pkg.getDependenciesInternal()) {
                    explicitDeps.add(dep.getName());
                }
                for (DependencyImpl stdDep : stdlibDependencies) {
                    if (!explicitDeps.contains(stdDep.getName()) && !stdInternalDeps.contains(stdDep.getPkgImpl().getId())) {
                        pkg.getDependenciesInternal().add(stdDep);
                    }
                }
            } else {
                PackageImplHelper.addDependencies(pkg, effectiveStdlib.getWorkspaceData(), newIdToPackage);
            }
        }

        return result;
    }

    private WorkspaceImpl withDependenciesOf(WorkspaceImpl other) {
        Map<String, PackageImpl> otherIdToPackage = new HashMap<>();
        for (PackageImpl pkg : other.myPackages) {
            otherIdToPackage.put(pkg.getId(), pkg);
        }
        Map<String, PackageImpl> thisIdToPackage = new HashMap<>();
        for (PackageImpl pkg : myPackages) {
            thisIdToPackage.put(pkg.getId(), pkg);
        }
        for (Map.Entry<String, PackageImpl> entry : thisIdToPackage.entrySet()) {
            String id = entry.getKey();
            PackageImpl pkg = entry.getValue();
            PackageImpl otherPkg = otherIdToPackage.get(id);
            if (otherPkg != null) {
                for (DependencyImpl dep : otherPkg.getDependenciesInternal()) {
                    PackageImpl dependencyPackage = thisIdToPackage.get(dep.getPkgImpl().getId());
                    if (dependencyPackage != null) {
                        pkg.getDependenciesInternal().add(dep.withPackage(dependencyPackage));
                    }
                }
            }
        }
        return this;
    }

    @Override
    public CargoWorkspace withDisabledFeatures(UserDisabledFeatures userDisabledFeatures) {
        Map<PackageFeature, FeatureState> featureState = inferFeatureState(userDisabledFeatures);
        Map<Path, Map<String, FeatureState>> featuresState = FeatureGraph.associateByPackageRoot(featureState);

        List<CargoWorkspaceData.Package> packagesData = new ArrayList<>();
        for (PackageImpl pkg : myPackages) {
            packagesData.add(pkg.asPackageData(null));
        }
        return new WorkspaceImpl(
            myManifestPath,
            myWorkspaceRootUrl,
            packagesData,
            myCfgOptions,
            myCargoConfig,
            featuresState
        ).withDependenciesOf(this);
    }

    private Map<PackageFeature, FeatureState> inferFeatureState(UserDisabledFeatures userDisabledFeatures) {
        Map<PackageFeature, FeatureState> workspaceFeatureState = getFeatureGraph().apply(FeatureState.Enabled, view -> {
            view.disableAll(userDisabledFeatures.getDisabledFeatures(getPackages()));
        });

        return getFeatureGraph().apply(FeatureState.Disabled, view -> {
            for (PackageImpl pkg : myPackages) {
                if (pkg.getOrigin() == PackageOrigin.WORKSPACE || pkg.getOrigin() == PackageOrigin.STDLIB) {
                    for (PackageFeature feature : pkg.getFeatures()) {
                        if (workspaceFeatureState.get(feature) == FeatureState.Enabled) {
                            view.enable(feature);
                        }
                    }
                }

                for (DependencyImpl dependency : pkg.getDependenciesInternal()) {
                    if (dependency.getPkgImpl().getOrigin() == PackageOrigin.WORKSPACE ||
                        dependency.getPkgImpl().getOrigin() == PackageOrigin.STDLIB) continue;
                    if (dependency.isAreDefaultFeaturesEnabled()) {
                        view.enable(new PackageFeature(dependency.getPkgImpl(), "default"));
                    }
                    List<PackageFeature> requiredFeatures = new ArrayList<>();
                    for (String f : dependency.getRequiredFeatures()) {
                        requiredFeatures.add(new PackageFeature(dependency.getPkgImpl(), f));
                    }
                    view.enableAll(requiredFeatures);
                }
            }
        });
    }

    @TestOnly
    @Override
    public CargoWorkspace withImplicitDependency(CargoWorkspaceData.Package pkgToAdd) {
        List<CargoWorkspaceData.Package> newPackagesData = new ArrayList<>();
        for (PackageImpl pkg : myPackages) {
            newPackagesData.add(pkg.asPackageData(null));
        }
        newPackagesData.add(pkgToAdd);

        WorkspaceImpl result = new WorkspaceImpl(
            myManifestPath,
            myWorkspaceRootUrl,
            newPackagesData,
            myCfgOptions,
            myCargoConfig,
            myFeaturesState
        );

        Map<String, PackageImpl> oldIdToPackage = new HashMap<>();
        for (PackageImpl pkg : myPackages) {
            oldIdToPackage.put(pkg.getId(), pkg);
        }
        Map<String, PackageImpl> newIdToPackage = new HashMap<>();
        for (PackageImpl pkg : result.myPackages) {
            newIdToPackage.put(pkg.getId(), pkg);
        }
        List<DependencyImpl> stdlibDependencies = new ArrayList<>();
        for (PackageImpl pkg : result.myPackages) {
            if (pkg.getOrigin() == PackageOrigin.STDLIB) {
                stdlibDependencies.add(new DependencyImpl(
                    pkg,
                    Collections.singletonList(new CargoWorkspace.DepKindInfo(CargoWorkspace.DepKind.Stdlib))
                ));
            }
        }
        PackageImpl pkgToAddImpl = newIdToPackage.get(pkgToAdd.getId());
        DependencyImpl pkgToAddDependency = new DependencyImpl(
            pkgToAddImpl,
            Collections.singletonList(new CargoWorkspace.DepKindInfo(CargoWorkspace.DepKind.Stdlib))
        );

        for (Map.Entry<String, PackageImpl> entry : newIdToPackage.entrySet()) {
            String id = entry.getKey();
            PackageImpl pkg = entry.getValue();
            if (id.equals(pkgToAdd.getId())) {
                pkg.getDependenciesInternal().addAll(stdlibDependencies);
            } else {
                PackageImpl oldPkg = oldIdToPackage.get(id);
                if (oldPkg != null) {
                    for (DependencyImpl dep : oldPkg.getDependenciesInternal()) {
                        PackageImpl dependencyPackage = newIdToPackage.get(dep.getPkgImpl().getId());
                        if (dependencyPackage != null) {
                            pkg.getDependenciesInternal().add(dep.withPackage(dependencyPackage));
                        }
                    }
                }
                if (pkg.getOrigin() != PackageOrigin.STDLIB && pkg.getOrigin() != PackageOrigin.STDLIB_DEPENDENCY) {
                    pkg.getDependenciesInternal().add(pkgToAddDependency);
                }
            }
        }

        return result;
    }

    @TestOnly
    @Override
    public CargoWorkspace withEdition(Edition edition) {
        List<CargoWorkspaceData.Package> packagesData = new ArrayList<>();
        for (PackageImpl pkg : myPackages) {
            Edition packageEdition = (pkg.getOrigin() == PackageOrigin.STDLIB || pkg.getOrigin() == PackageOrigin.STDLIB_DEPENDENCY)
                ? pkg.getEdition() : edition;
            packagesData.add(pkg.asPackageData(packageEdition));
        }
        return new WorkspaceImpl(
            myManifestPath,
            myWorkspaceRootUrl,
            packagesData,
            myCfgOptions,
            myCargoConfig,
            myFeaturesState
        ).withDependenciesOf(this);
    }

    @TestOnly
    @Override
    public CargoWorkspace withCfgOptions(CfgOptions cfgOptions) {
        List<CargoWorkspaceData.Package> packagesData = new ArrayList<>();
        for (PackageImpl pkg : myPackages) {
            packagesData.add(pkg.asPackageData(null));
        }
        return new WorkspaceImpl(
            myManifestPath,
            myWorkspaceRootUrl,
            packagesData,
            cfgOptions,
            myCargoConfig,
            myFeaturesState
        ).withDependenciesOf(this);
    }

    @TestOnly
    @Override
    public CargoWorkspace withCargoFeatures(Map<PackageFeature, List<String>> features) {
        Map<CargoWorkspace.Package, Map<String, List<String>>> packageToFeatures = new HashMap<>();
        for (Map.Entry<PackageFeature, List<String>> entry : features.entrySet()) {
            packageToFeatures
                .computeIfAbsent(entry.getKey().getPkg(), k -> new HashMap<>())
                .put(entry.getKey().getName(), entry.getValue());
        }

        List<CargoWorkspaceData.Package> packagesData = new ArrayList<>();
        for (PackageImpl pkg : myPackages) {
            Map<String, List<String>> pkgFeatures = packageToFeatures.getOrDefault(pkg, Collections.emptyMap());
            CargoWorkspaceData.Package data = pkg.asPackageData(null)
                .copyWithFeaturesAndEnabledFeatures(pkgFeatures, pkgFeatures.keySet());
            packagesData.add(data);
        }
        return new WorkspaceImpl(
            myManifestPath,
            myWorkspaceRootUrl,
            packagesData,
            myCfgOptions,
            myCargoConfig,
            myFeaturesState
        ).withDependenciesOf(this).withDisabledFeatures(UserDisabledFeatures.EMPTY);
    }

    @Override
    public String toString() {
        StringBuilder pkgs = new StringBuilder();
        for (PackageImpl pkg : myPackages) {
            pkgs.append("    ").append(pkg).append(",\n");
        }
        return "Workspace(packages=[\n" + pkgs + "])";
    }

    static WorkspaceImpl deserialize(
        Path manifestPath,
        CargoWorkspaceData data,
        CfgOptions cfgOptions,
        CargoConfig cargoConfig
    ) {
        WorkspaceImpl result = new WorkspaceImpl(
            manifestPath,
            data.getWorkspaceRootUrl(),
            data.getPackages(),
            cfgOptions,
            cargoConfig,
            Collections.emptyMap()
        );

        Map<String, PackageImpl> idToPackage = new HashMap<>();
        for (PackageImpl pkg : result.myPackages) {
            idToPackage.put(pkg.getId(), pkg);
        }
        for (PackageImpl pkg : result.myPackages) {
            PackageImplHelper.addDependencies(pkg, data, idToPackage);
        }

        return result;
    }
}
