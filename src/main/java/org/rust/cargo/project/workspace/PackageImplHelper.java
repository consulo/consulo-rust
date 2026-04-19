/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.cargo.project.workspace;

import java.util.*;

final class PackageImplHelper {

    private PackageImplHelper() {}

    static void addDependencies(
        PackageImpl pkg,
        CargoWorkspaceData workspaceData,
        Map<String, PackageImpl> packagesMap
    ) {
        Set<CargoWorkspaceData.Dependency> pkgDeps = workspaceData.getDependencies().get(pkg.getId());
        if (pkgDeps == null) pkgDeps = Collections.emptySet();
        List<org.rust.cargo.toolchain.impl.CargoMetadata.RawDependency> pkgRawDeps =
            workspaceData.getRawDependencies().get(pkg.getId());
        if (pkgRawDeps == null) pkgRawDeps = Collections.emptyList();

        for (CargoWorkspaceData.Dependency dep : pkgDeps) {
            PackageImpl dependencyPackage = packagesMap.get(dep.getId());
            if (dependencyPackage == null) continue;

            CargoWorkspace.Target libTarget = dependencyPackage.getLibTarget();
            String depTargetName = libTarget != null ? libTarget.getNormName() : dependencyPackage.getNormName();
            String depName = dep.getName() != null ? dep.getName() : depTargetName;
            String rename = !depName.equals(depTargetName) ? depName : null;

            List<org.rust.cargo.toolchain.impl.CargoMetadata.RawDependency> rawDeps = new ArrayList<>();
            for (org.rust.cargo.toolchain.impl.CargoMetadata.RawDependency rawDep : pkgRawDeps) {
                if (rawDep.getName().equals(dependencyPackage.getName())) {
                    String rawRename = rawDep.getRename() != null ? rawDep.getRename().replace('-', '_') : null;
                    if (Objects.equals(rawRename, rename)) {
                        boolean kindsMatch = false;
                        for (CargoWorkspace.DepKindInfo kindInfo : dep.getDepKinds()) {
                            if (kindInfo.getKind() == CargoWorkspace.DepKind.Unclassified) {
                                kindsMatch = true;
                                break;
                            }
                            if (Objects.equals(kindInfo.getTarget(), rawDep.getTarget()) &&
                                Objects.equals(kindInfo.getKind().getCargoName(), rawDep.getKind())) {
                                kindsMatch = true;
                                break;
                            }
                        }
                        if (kindsMatch) {
                            rawDeps.add(rawDep);
                        }
                    }
                }
            }

            boolean isOptional = false;
            boolean usesDefaultFeatures = false;
            Set<String> requiredFeatures = new HashSet<>();
            String cargoFeatureDependencyPackageName = dependencyPackage.getName();

            for (org.rust.cargo.toolchain.impl.CargoMetadata.RawDependency rawDep : rawDeps) {
                if (rawDep.isOptional()) isOptional = true;
                if (rawDep.isUses_default_features()) usesDefaultFeatures = true;
                requiredFeatures.addAll(rawDep.getFeatures());
            }

            if (!rawDeps.isEmpty()) {
                String firstRename = rawDeps.get(0).getRename();
                cargoFeatureDependencyPackageName = firstRename != null ? firstRename : dependencyPackage.getName();
            }

            pkg.getDependenciesInternal().add(new DependencyImpl(
                dependencyPackage,
                depName,
                dep.getDepKinds(),
                isOptional,
                usesDefaultFeatures,
                requiredFeatures,
                cargoFeatureDependencyPackageName
            ));
        }
    }
}
