/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.inspections;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.rust.ide.fixes.AddMutableFix;
import org.rust.lang.core.psi.*;
import org.rust.lang.core.psi.ext.RsExprUtil;
import org.rust.lang.core.psi.ext.RsIndexExprUtil;
import org.rust.lang.core.psi.ext.RsUnaryExprUtil;
import org.rust.lang.core.types.RsTypesUtil;
import org.rust.lang.core.types.ty.Ty;
import org.rust.lang.core.types.ty.TyPointer;
import org.rust.lang.core.types.ty.TyReference;
import org.rust.lang.utils.RsDiagnostic;
import org.rust.lang.core.types.ExtensionsUtil;

public class RsAssignToImmutableInspection extends RsLocalInspectionTool {

    @Override
    public RsVisitor buildVisitor(@NotNull RsProblemsHolder holder, boolean isOnTheFly) {
        return new RsWithMacrosInspectionVisitor() {
            @Override
            public void visitBinaryExpr(@NotNull RsBinaryExpr expr) {
                if (RsExprUtil.isAssignBinaryExpr(expr)) {
                    checkAssignment(expr, holder);
                }
            }
        };
    }

    private void checkAssignment(@NotNull RsBinaryExpr expr, @NotNull RsProblemsHolder holder) {
        RsExpr leftRaw = RsExprUtil.unwrapParenExprs(expr.getLeft());
        if (leftRaw == null || !ExtensionsUtil.isImmutable(leftRaw)) return;

        if (leftRaw instanceof RsDotExpr) {
            registerProblem(holder, "field of immutable binding", expr, ((RsDotExpr) leftRaw).getExpr());
        } else if (leftRaw instanceof RsIndexExpr) {
            registerProblem(holder, "indexed content of immutable binding", expr, RsIndexExprUtil.getContainerExpr((RsIndexExpr) leftRaw));
        } else if (leftRaw instanceof RsUnaryExpr) {
            RsUnaryExpr unary = (RsUnaryExpr) leftRaw;
            if (RsUnaryExprUtil.isDereference(unary)) {
                registerDereferenceProblem(unary, holder, expr);
            }
        }
    }

    private void registerDereferenceProblem(@NotNull RsUnaryExpr left, @NotNull RsProblemsHolder holder, @NotNull RsBinaryExpr expr) {
        RsExpr inner = left.getExpr();
        if (inner == null) return;
        Ty type = RsTypesUtil.getType(inner);
        if (type instanceof TyReference) {
            registerProblem(holder, "immutable borrowed content", expr, null);
        } else if (type instanceof TyPointer) {
            registerProblem(holder, "immutable dereference of raw pointer", expr, null);
        }
    }

    private void registerProblem(@NotNull RsProblemsHolder holder, @NotNull String message, @NotNull RsExpr expr, @Nullable RsExpr assigneeExpr) {
        AddMutableFix fix = assigneeExpr != null ? AddMutableFix.createIfCompatible(assigneeExpr) : null;
        RsDiagnostic.addToHolder(new RsDiagnostic.CannotAssignToImmutable(expr, message, fix), holder);
    }
}
