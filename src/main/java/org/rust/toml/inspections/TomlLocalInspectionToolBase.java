/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.toml.inspections;

import com.intellij.codeInspection.LocalInspectionTool;
import com.intellij.codeInspection.ProblemsHolder;
import com.intellij.psi.PsiElementVisitor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.rust.toml.Util;

public abstract class TomlLocalInspectionToolBase extends LocalInspectionTool {

    @NotNull
    @Override
    public final PsiElementVisitor buildVisitor(@NotNull ProblemsHolder holder, boolean isOnTheFly) {
        if (!Util.tomlPluginIsAbiCompatible()) return super.buildVisitor(holder, isOnTheFly);
        PsiElementVisitor visitor = buildVisitorInternal(holder, isOnTheFly);
        return visitor != null ? visitor : super.buildVisitor(holder, isOnTheFly);
    }

    @Nullable
    protected abstract PsiElementVisitor buildVisitorInternal(@NotNull ProblemsHolder holder, boolean isOnTheFly);
}
