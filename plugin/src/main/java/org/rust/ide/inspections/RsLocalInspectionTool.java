/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.inspections;

import consulo.language.editor.inspection.LocalInspectionTool;
import consulo.language.editor.inspection.LocalInspectionToolSession;
import consulo.language.editor.inspection.ProblemsHolder;
import consulo.language.psi.PsiElementVisitor;
import consulo.language.psi.PsiFile;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.rust.cargo.project.settings.RsProjectSettingsServiceUtil;
import org.rust.lang.core.psi.RsFile;
import org.rust.lang.core.psi.RsVisitor;
import org.rust.openapiext.OpenApiUtil;
import consulo.language.Language;
import consulo.language.editor.rawHighlight.HighlightDisplayLevel;
import consulo.localize.LocalizeValue;
import org.rust.lang.RsLanguage;

public abstract class RsLocalInspectionTool extends LocalInspectionTool {

    @Nonnull
    @Override
    public final PsiElementVisitor buildVisitor(
        @Nonnull ProblemsHolder holder,
        boolean isOnTheFly,
        @Nonnull LocalInspectionToolSession session,
        @Nonnull Object state
    ) {
        PsiFile file = session.getFile();
        if (file instanceof RsFile && isApplicableTo((RsFile) file)) {
            return buildVisitor(holder, isOnTheFly);
        } else {
            return PsiElementVisitor.EMPTY_VISITOR;
        }
    }

    @Nonnull
    @Override
    public final PsiElementVisitor buildVisitor(@Nonnull ProblemsHolder holder, boolean isOnTheFly) {
        RsVisitor visitor = buildVisitor(new RsProblemsHolder(holder), isOnTheFly);
        if (visitor != null) {
            return visitor;
        }
        return super.buildVisitor(holder, isOnTheFly);
    }

    @Nullable
    public RsVisitor buildVisitor(@Nonnull RsProblemsHolder holder, boolean isOnTheFly) {
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
    private boolean isApplicableTo(@Nonnull RsFile file) {
        if (isSyntaxOnly()) return true;
        if (!file.isDeeplyEnabledByCfg()) return false;

        if (OpenApiUtil.isUnitTestMode()) return true;

        return file.getCargoWorkspace() != null
            && file.getCrateRoot() != null
            && RsProjectSettingsServiceUtil.getToolchain(file.getProject()) != null;
    }

    @Override
    @jakarta.annotation.Nonnull
    public consulo.language.editor.rawHighlight.HighlightDisplayLevel getDefaultLevel() {
        return consulo.language.editor.rawHighlight.HighlightDisplayLevel.WARNING;
    }

    @Override
    @jakarta.annotation.Nonnull
    public consulo.localize.LocalizeValue getDisplayName() {
        return consulo.localize.LocalizeValue.of(getClass().getSimpleName());
    }

    @Override
    @jakarta.annotation.Nonnull
    public consulo.localize.LocalizeValue getGroupDisplayName() {
        return consulo.localize.LocalizeValue.of("Rust");
    }

    @Override
    @jakarta.annotation.Nonnull
    public consulo.language.Language getLanguage() {
        return org.rust.lang.RsLanguage.INSTANCE;
    }
}
