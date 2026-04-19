/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.toml.resolve;

import com.intellij.patterns.StandardPatterns;
import com.intellij.psi.PsiReferenceContributor;
import com.intellij.psi.PsiReferenceRegistrar;
import org.jetbrains.annotations.NotNull;
import org.rust.toml.CargoTomlPsiPattern;
import org.rust.toml.Util;

/**
 * Provides references for TOML elements in {@code Cargo.toml} files.
 */
public class CargoTomlReferenceContributor extends PsiReferenceContributor {
    @Override
    public void registerReferenceProviders(@NotNull PsiReferenceRegistrar registrar) {
        if (Util.tomlPluginIsAbiCompatible()) {
            registrar.registerReferenceProvider(
                StandardPatterns.or(CargoTomlPsiPattern.INSTANCE.getOnDependencyKey(), CargoTomlPsiPattern.INSTANCE.getOnSpecificDependencyHeaderKey()),
                new CargoDependencyReferenceProvider()
            );
            for (PathPatternType type : PathPatternType.values()) {
                registrar.registerReferenceProvider(type.getPattern(), new CargoTomlFileReferenceProvider(type));
            }
            registrar.registerReferenceProvider(CargoTomlPsiPattern.INSTANCE.getOnFeatureDependencyLiteral(), new CargoTomlFeatureDependencyReferenceProvider());
            registrar.registerReferenceProvider(CargoTomlPsiPattern.INSTANCE.getOnDependencyPackageFeature(), new CargoTomlDependencyFeaturesReferenceProvider());
        }
    }
}
