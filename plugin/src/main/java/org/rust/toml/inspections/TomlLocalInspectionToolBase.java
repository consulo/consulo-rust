/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.toml.inspections;

import consulo.language.editor.inspection.LocalInspectionTool;
import consulo.language.editor.inspection.ProblemsHolder;
import consulo.language.psi.PsiElementVisitor;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.rust.toml.Util;

public abstract class TomlLocalInspectionToolBase extends LocalInspectionTool {

    @Nonnull
    @Override
    public final PsiElementVisitor buildVisitor(@Nonnull ProblemsHolder holder, boolean isOnTheFly) {
        if (!Util.tomlPluginIsAbiCompatible()) return super.buildVisitor(holder, isOnTheFly);
        PsiElementVisitor visitor = buildVisitorInternal(holder, isOnTheFly);
        return visitor != null ? visitor : super.buildVisitor(holder, isOnTheFly);
    }

    @Nullable
    protected abstract PsiElementVisitor buildVisitorInternal(@Nonnull ProblemsHolder holder, boolean isOnTheFly);

    @Nonnull
    @Override
    public consulo.language.editor.rawHighlight.HighlightDisplayLevel getDefaultLevel() {
        return consulo.language.editor.rawHighlight.HighlightDisplayLevel.WARNING;
    }
}
