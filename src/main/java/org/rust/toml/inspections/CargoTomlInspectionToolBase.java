/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.toml.inspections;

import com.intellij.codeInspection.ProblemsHolder;
import com.intellij.psi.PsiElementVisitor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.rust.cargo.CargoConstants;
import org.rust.ide.experiments.RsExperiments;
import org.rust.openapiext.OpenApiUtil;
import org.toml.lang.psi.TomlVisitor;

public abstract class CargoTomlInspectionToolBase extends TomlLocalInspectionToolBase {
    protected boolean requiresLocalCrateIndex() {
        return false;
    }

    @NotNull
    protected abstract TomlVisitor buildCargoTomlVisitor(@NotNull ProblemsHolder holder);

    @Nullable
    @Override
    protected PsiElementVisitor buildVisitorInternal(@NotNull ProblemsHolder holder, boolean isOnTheFly) {
        if (requiresLocalCrateIndex() && !OpenApiUtil.isFeatureEnabled(RsExperiments.CRATES_LOCAL_INDEX)) return null;
        if (!CargoConstants.MANIFEST_FILE.equals(holder.getFile().getName())) return null;
        return buildCargoTomlVisitor(holder);
    }
}
