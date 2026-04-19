/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.inspections;

import com.intellij.codeInspection.LocalInspectionTool;
import com.intellij.codeInspection.LocalInspectionToolSession;
import com.intellij.codeInspection.ProblemsHolder;
import com.intellij.psi.PsiElementVisitor;
import com.intellij.psi.PsiFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.rust.cargo.project.settings.RsProjectSettingsServiceUtil;
import org.rust.lang.core.psi.RsFile;
import org.rust.lang.core.psi.RsVisitor;
import org.rust.openapiext.OpenApiUtil;

public abstract class RsLocalInspectionTool extends LocalInspectionTool {

    @NotNull
    @Override
    public final PsiElementVisitor buildVisitor(
        @NotNull ProblemsHolder holder,
        boolean isOnTheFly,
        @NotNull LocalInspectionToolSession session
    ) {
        PsiFile file = session.getFile();
        if (file instanceof RsFile && isApplicableTo((RsFile) file)) {
            return buildVisitor(holder, isOnTheFly);
        } else {
            return PsiElementVisitor.EMPTY_VISITOR;
        }
    }

    @NotNull
    @Override
    public final PsiElementVisitor buildVisitor(@NotNull ProblemsHolder holder, boolean isOnTheFly) {
        RsVisitor visitor = buildVisitor(new RsProblemsHolder(holder), isOnTheFly);
        if (visitor != null) {
            return visitor;
        }
        return super.buildVisitor(holder, isOnTheFly);
    }

    @Nullable
    public RsVisitor buildVisitor(@NotNull RsProblemsHolder holder, boolean isOnTheFly) {
        return null;
    }

    public boolean isSyntaxOnly() {
        return false;
    }

    /**
     * Syntax-only inspections are applicable to any {@link RsFile}.
     *
     * Other inspections should analyze only files that:
     * - belong to a workspace
     * - are included in module tree, i.e. have a crate root
     * - are not disabled with a {@code cfg} attribute
     * - belong to a project with a configured and valid Rust toolchain
     */
    private boolean isApplicableTo(@NotNull RsFile file) {
        if (isSyntaxOnly()) return true;
        if (!file.isDeeplyEnabledByCfg()) return false;

        if (OpenApiUtil.isUnitTestMode()) return true;

        return file.getCargoWorkspace() != null
            && file.getCrateRoot() != null
            && RsProjectSettingsServiceUtil.getToolchain(file.getProject()) != null;
    }
}
