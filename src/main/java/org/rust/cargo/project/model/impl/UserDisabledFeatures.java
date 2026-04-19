/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.cargo.project.model.impl;

import org.jetbrains.annotations.NotNull;
import org.rust.cargo.project.workspace.CargoWorkspace;
import org.rust.cargo.project.workspace.FeatureState;
import org.rust.cargo.project.workspace.PackageFeature;

import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;

public abstract class UserDisabledFeatures {

    public static final UserDisabledFeatures EMPTY = new ImmutableUserDisabledFeatures(Collections.emptyMap());

    @NotNull
    public abstract Map<Path, Set<String>> getPkgRootToDisabledFeatures();

    @NotNull
    public List<PackageFeature> getDisabledFeatures(@NotNull Iterable<CargoWorkspace.Package> packages) {
        List<PackageFeature> result = new ArrayList<>();
        for (CargoWorkspace.Package pkg : packages) {
            Set<String> disabledNames = getPkgRootToDisabledFeatures().get(pkg.getRootDirectory());
            if (disabledNames != null) {
                for (String name : disabledNames) {
                    PackageFeature feature = new PackageFeature(pkg, name);
                    if (pkg.getFeatures().contains(feature)) {
                        result.add(feature);
                    }
                }
            }
        }
        return result;
    }

    public boolean isEmpty() {
        if (getPkgRootToDisabledFeatures().isEmpty()) return true;
        return getPkgRootToDisabledFeatures().values().stream().allMatch(Set::isEmpty);
    }

    @NotNull
    public MutableUserDisabledFeatures toMutable() {
        Map<Path, Set<String>> mutableMap = new HashMap<>();
        for (var entry : getPkgRootToDisabledFeatures().entrySet()) {
            mutableMap.put(entry.getKey(), new HashSet<>(entry.getValue()));
        }
        return new MutableUserDisabledFeatures(mutableMap);
    }

    @NotNull
    public UserDisabledFeatures retain(@NotNull Iterable<CargoWorkspace.Package> packages) {
        MutableUserDisabledFeatures newMap = EMPTY.toMutable();
        for (PackageFeature disabledFeature : getDisabledFeatures(packages)) {
            newMap.setFeatureState(disabledFeature, FeatureState.Disabled);
        }
        return newMap;
    }

    @NotNull
    public static UserDisabledFeatures of(@NotNull Map<Path, Set<String>> pkgRootToDisabledFeatures) {
        return new ImmutableUserDisabledFeatures(pkgRootToDisabledFeatures);
    }

    private static class ImmutableUserDisabledFeatures extends UserDisabledFeatures {
        private final Map<Path, Set<String>> pkgRootToDisabledFeatures;

        ImmutableUserDisabledFeatures(@NotNull Map<Path, Set<String>> pkgRootToDisabledFeatures) {
            this.pkgRootToDisabledFeatures = Collections.unmodifiableMap(pkgRootToDisabledFeatures);
        }

        @NotNull
        @Override
        public Map<Path, Set<String>> getPkgRootToDisabledFeatures() {
            return pkgRootToDisabledFeatures;
        }
    }
}
