/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.cargo.project.model.impl;

import org.jetbrains.annotations.NotNull;
import org.rust.cargo.project.workspace.FeatureState;
import org.rust.cargo.project.workspace.PackageFeature;

import java.nio.file.Path;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class MutableUserDisabledFeatures extends UserDisabledFeatures {

    private final Map<Path, Set<String>> pkgRootToDisabledFeatures;

    public MutableUserDisabledFeatures(@NotNull Map<Path, Set<String>> pkgRootToDisabledFeatures) {
        this.pkgRootToDisabledFeatures = pkgRootToDisabledFeatures;
    }

    @NotNull
    @Override
    public Map<Path, Set<String>> getPkgRootToDisabledFeatures() {
        return pkgRootToDisabledFeatures;
    }

    public void setFeatureState(@NotNull PackageFeature feature, @NotNull FeatureState state) {
        Path packageRoot = feature.getPkg().getRootDirectory();
        switch (state) {
            case Enabled -> {
                Set<String> features = pkgRootToDisabledFeatures.get(packageRoot);
                if (features != null) {
                    features.remove(feature.getName());
                }
            }
            case Disabled -> pkgRootToDisabledFeatures
                .computeIfAbsent(packageRoot, k -> new HashSet<>())
                .add(feature.getName());
        }
    }
}
