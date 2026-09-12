/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.toml.search;

import consulo.annotation.component.ExtensionImpl;
import consulo.language.psi.PsiElement;
import consulo.usage.UsageTarget;
import consulo.usage.UsageType;
import consulo.usage.UsageTypeProviderEx;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.rust.lang.core.RsPsiPattern;
import org.rust.lang.core.psi.RsLitExpr;
import org.rust.toml.CargoTomlPsiPattern;
import org.rust.toml.Util;
import org.toml.lang.psi.TomlLiteral;
import consulo.localize.LocalizeValue;

@ExtensionImpl
public class CargoTomlUsageTypeProvider implements UsageTypeProviderEx {
    private static final UsageType FEATURE_DEPENDENCY = new UsageType(consulo.localize.LocalizeValue.of("Cargo feature dependency"));
    private static final UsageType DEPENDENCY_FEATURE = new UsageType(consulo.localize.LocalizeValue.of("Package dependency"));
    private static final UsageType CFG_FEATURE = new UsageType(consulo.localize.LocalizeValue.of("Cfg attribute"));

    @Nullable
    @Override
    public UsageType getUsageType(@Nonnull PsiElement element) {
        return getUsageType(element, UsageTarget.EMPTY_ARRAY);
    }

    @Nullable
    @Override
    public UsageType getUsageType(PsiElement element, @Nonnull UsageTarget[] targets) {
        if (!Util.tomlPluginIsAbiCompatible()) return null;
        if (element instanceof TomlLiteral) {
            if (CargoTomlPsiPattern.INSTANCE.getOnFeatureDependencyLiteral().accepts(element)) {
                return FEATURE_DEPENDENCY;
            }
            return DEPENDENCY_FEATURE;
        }
        if (element instanceof RsLitExpr) {
            if (RsPsiPattern.anyCfgFeature.accepts(element)) {
                return CFG_FEATURE;
            }
            return null;
        }
        return null;
    }
}
