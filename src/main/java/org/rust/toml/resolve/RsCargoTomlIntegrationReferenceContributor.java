/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.toml.resolve;

import com.intellij.psi.PsiReferenceContributor;
import com.intellij.psi.PsiReferenceRegistrar;
import org.jetbrains.annotations.NotNull;
import org.rust.lang.core.RsPsiPattern;
import org.rust.toml.Util;

/**
 * Provides references (that point to TOML elements) for Rust elements in Rust files.
 */
public class RsCargoTomlIntegrationReferenceContributor extends PsiReferenceContributor {
    @Override
    public void registerReferenceProviders(@NotNull PsiReferenceRegistrar registrar) {
        if (Util.tomlPluginIsAbiCompatible()) {
            registrar.registerReferenceProvider(RsPsiPattern.anyCfgFeature, new RsCfgFeatureReferenceProvider());
        }
    }
}
