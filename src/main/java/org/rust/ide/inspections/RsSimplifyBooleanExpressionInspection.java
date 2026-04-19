/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.inspections;

import org.jetbrains.annotations.NotNull;
import org.rust.RsBundle;
import org.rust.ide.fixes.SimplifyBooleanExpressionFix;
import org.rust.ide.utils.BooleanExprSimplifier;
import org.rust.ide.utils.PurityUtil;
import org.rust.lang.core.psi.RsExpr;
import org.rust.lang.core.psi.RsVisitor;
import org.rust.ide.utils.ExprUtil;

/**
 * Simplify pure boolean expressions
 */
public class RsSimplifyBooleanExpressionInspection extends RsLocalInspectionTool {

    @Override
    public String getDisplayName() {
        return RsBundle.message("intention.name.simplify.boolean.expression");
    }

    @Override
    public RsVisitor buildVisitor(@NotNull RsProblemsHolder holder, boolean isOnTheFly) {
        return new RsWithMacrosInspectionVisitor() {
            @Override
            public void visitExpr(@NotNull RsExpr expr) {
                Boolean isPure = ExprUtil.isPure(expr);
                if (isPure != null && isPure && BooleanExprSimplifier.canBeSimplified(expr)) {
                    holder.registerProblem(expr, RsBundle.message("inspection.message.boolean.expression.can.be.simplified"), new SimplifyBooleanExpressionFix(expr));
                }
            }
        };
    }
}
