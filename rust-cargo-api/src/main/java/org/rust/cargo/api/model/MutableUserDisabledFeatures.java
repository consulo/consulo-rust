/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.cargo.api.model;

import org.rust.cargo.api.model.UserDisabledFeatures;

import jakarta.annotation.Nonnull;
import org.rust.cargo.api.workspace.FeatureState;
import org.rust.cargo.api.workspace.PackageFeature;

import java.nio.file.Path;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class MutableUserDisabledFeatures extends UserDisabledFeatures {

    private final Map<Path, Set<String>> pkgRootToDisabledFeatures;

    public MutableUserDisabledFeatures(@Nonnull Map<Path, Set<String>> pkgRootToDisabledFeatures) {
        this.pkgRootToDisabledFeatures = pkgRootToDisabledFeatures;
    }

    @Nonnull
    @Override
    public Map<Path, Set<String>> getPkgRootToDisabledFeatures() {
        return pkgRootToDisabledFeatures;
    }

    public void setFeatureState(@Nonnull PackageFeature feature, @Nonnull FeatureState state) {
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
