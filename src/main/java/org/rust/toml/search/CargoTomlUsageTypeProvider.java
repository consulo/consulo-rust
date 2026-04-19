/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.toml.search;

import com.intellij.psi.PsiElement;
import com.intellij.usages.UsageTarget;
import com.intellij.usages.impl.rules.UsageType;
import com.intellij.usages.impl.rules.UsageTypeProviderEx;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.rust.lang.core.RsPsiPattern;
import org.rust.lang.core.psi.RsLitExpr;
import org.rust.toml.CargoTomlPsiPattern;
import org.rust.toml.Util;
import org.toml.lang.psi.TomlLiteral;

public class CargoTomlUsageTypeProvider implements UsageTypeProviderEx {
    private static final UsageType FEATURE_DEPENDENCY = new UsageType(() -> "Cargo feature dependency");
    private static final UsageType DEPENDENCY_FEATURE = new UsageType(() -> "Package dependency");
    private static final UsageType CFG_FEATURE = new UsageType(() -> "Cfg attribute");

    @Nullable
    @Override
    public UsageType getUsageType(@NotNull PsiElement element) {
        return getUsageType(element, UsageTarget.EMPTY_ARRAY);
    }

    @Nullable
    @Override
    public UsageType getUsageType(PsiElement element, @NotNull UsageTarget[] targets) {
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
