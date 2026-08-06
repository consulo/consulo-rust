/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.cargo.project.model.impl;

import jakarta.annotation.Nonnull;
import org.rust.cargo.project.workspace.CargoWorkspace;
import org.rust.cargo.project.workspace.FeatureState;
import org.rust.cargo.project.workspace.PackageFeature;

import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;

public abstract class UserDisabledFeatures {

    public static final UserDisabledFeatures EMPTY = new ImmutableUserDisabledFeatures(Collections.emptyMap());

    @Nonnull
    public abstract Map<Path, Set<String>> getPkgRootToDisabledFeatures();

    @Nonnull
    public List<PackageFeature> getDisabledFeatures(@Nonnull Iterable<CargoWorkspace.Package> packages) {
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

    @Nonnull
    public MutableUserDisabledFeatures toMutable() {
        Map<Path, Set<String>> mutableMap = new HashMap<>();
        for (var entry : getPkgRootToDisabledFeatures().entrySet()) {
            mutableMap.put(entry.getKey(), new HashSet<>(entry.getValue()));
        }
        return new MutableUserDisabledFeatures(mutableMap);
    }

    @Nonnull
    public UserDisabledFeatures retain(@Nonnull Iterable<CargoWorkspace.Package> packages) {
        MutableUserDisabledFeatures newMap = EMPTY.toMutable();
        for (PackageFeature disabledFeature : getDisabledFeatures(packages)) {
            newMap.setFeatureState(disabledFeature, FeatureState.Disabled);
        }
        return newMap;
    }

    @Nonnull
    public static UserDisabledFeatures of(@Nonnull Map<Path, Set<String>> pkgRootToDisabledFeatures) {
        return new ImmutableUserDisabledFeatures(pkgRootToDisabledFeatures);
    }

    private static class ImmutableUserDisabledFeatures extends UserDisabledFeatures {
        private final Map<Path, Set<String>> pkgRootToDisabledFeatures;

        ImmutableUserDisabledFeatures(@Nonnull Map<Path, Set<String>> pkgRootToDisabledFeatures) {
            this.pkgRootToDisabledFeatures = Collections.unmodifiableMap(pkgRootToDisabledFeatures);
        }

        @Nonnull
        @Override
        public Map<Path, Set<String>> getPkgRootToDisabledFeatures() {
            return pkgRootToDisabledFeatures;
        }
    }
}
