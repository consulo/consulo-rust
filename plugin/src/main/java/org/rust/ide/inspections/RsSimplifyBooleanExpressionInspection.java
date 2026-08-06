/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.inspections;

import jakarta.annotation.Nonnull;
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
    public RsVisitor buildVisitor(@Nonnull RsProblemsHolder holder, boolean isOnTheFly) {
        return new RsWithMacrosInspectionVisitor() {
            @Override
            public void visitExpr(@Nonnull RsExpr expr) {
                Boolean isPure = ExprUtil.isPure(expr);
                if (isPure != null && isPure && BooleanExprSimplifier.canBeSimplified(expr)) {
                    holder.registerProblem(expr, RsBundle.message("inspection.message.boolean.expression.can.be.simplified"), new SimplifyBooleanExpressionFix(expr));
                }
            }
        };
    }

    @Override
    @jakarta.annotation.Nonnull
    public consulo.localize.LocalizeValue getDisplayName() {
        return consulo.localize.LocalizeValue.of(org.rust.RsBundle.message("inspection.rs.simplify.boolean.expression.display.name"));
    }

    @Override
    @jakarta.annotation.Nonnull
    public consulo.localize.LocalizeValue getGroupDisplayName() {
        return consulo.localize.LocalizeValue.of(org.rust.RsBundle.message("rust"));
    }
}
