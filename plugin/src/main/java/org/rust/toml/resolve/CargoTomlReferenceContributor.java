/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.toml.resolve;

import consulo.language.pattern.StandardPatterns;
import consulo.language.psi.PsiReferenceContributor;
import consulo.language.psi.PsiReferenceRegistrar;
import jakarta.annotation.Nonnull;
import org.rust.toml.CargoTomlPsiPattern;
import org.rust.toml.Util;
import consulo.annotation.component.ExtensionImpl;
import consulo.language.Language;

/**
 * Provides references for TOML elements in {@code Cargo.toml} files.
 */
@ExtensionImpl
public class CargoTomlReferenceContributor extends PsiReferenceContributor {
    @jakarta.annotation.Nonnull @Override public consulo.language.Language getLanguage() { return org.toml.lang.TomlLanguage.INSTANCE; }

    @Override
    public void registerReferenceProviders(@Nonnull PsiReferenceRegistrar registrar) {
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
