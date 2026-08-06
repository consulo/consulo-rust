/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.toml.inspections;

import consulo.language.editor.inspection.ProblemsHolder;
import consulo.language.psi.PsiElementVisitor;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.rust.cargo.CargoConstants;
import org.rust.ide.experiments.RsExperiments;
import org.rust.openapiext.OpenApiUtil;
import org.toml.lang.psi.TomlVisitor;

public abstract class CargoTomlInspectionToolBase extends TomlLocalInspectionToolBase {
    protected boolean requiresLocalCrateIndex() {
        return false;
    }

    @Nonnull
    protected abstract TomlVisitor buildCargoTomlVisitor(@Nonnull ProblemsHolder holder);

    @Nullable
    @Override
    protected PsiElementVisitor buildVisitorInternal(@Nonnull ProblemsHolder holder, boolean isOnTheFly) {
        if (requiresLocalCrateIndex() && !OpenApiUtil.isFeatureEnabled(RsExperiments.CRATES_LOCAL_INDEX)) return null;
        if (!CargoConstants.MANIFEST_FILE.equals(holder.getFile().getName())) return null;
        return buildCargoTomlVisitor(holder);
    }
}
