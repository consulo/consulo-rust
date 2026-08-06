/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.toml.resolve;

import consulo.language.psi.PsiReferenceContributor;
import consulo.language.psi.PsiReferenceRegistrar;
import jakarta.annotation.Nonnull;
import org.rust.lang.core.RsPsiPattern;
import org.rust.toml.Util;

/**
 * Provides references (that point to TOML elements) for Rust elements in Rust files.
 */
public class RsCargoTomlIntegrationReferenceContributor extends PsiReferenceContributor {
    @jakarta.annotation.Nonnull @Override public consulo.language.Language getLanguage() { return org.toml.lang.TomlLanguage.INSTANCE; }

    @Override
    public void registerReferenceProviders(@Nonnull PsiReferenceRegistrar registrar) {
        if (Util.tomlPluginIsAbiCompatible()) {
            registrar.registerReferenceProvider(RsPsiPattern.anyCfgFeature, new RsCfgFeatureReferenceProvider());
        }
    }
}
