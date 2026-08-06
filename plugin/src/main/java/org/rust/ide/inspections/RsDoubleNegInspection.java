/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.inspections;

import jakarta.annotation.Nonnull;
import org.rust.RsBundle;
import org.rust.ide.fixes.ReplaceIncDecOperatorFix;
import org.rust.lang.core.psi.RsExpr;
import org.rust.lang.core.psi.RsUnaryExpr;
import org.rust.lang.core.psi.RsVisitor;

import consulo.language.editor.inspection.LocalQuickFix;
import consulo.language.psi.PsiElement;

import java.util.ArrayList;
import java.util.List;

/**
 * Checks for usage of double negation, which is a no-op in Rust but might be misleading for
 * programmers with background in languages that have prefix operators.
 *
 * Analogue of Clippy's double_neg.
 */
public class RsDoubleNegInspection extends RsLocalInspectionTool {

@Override
    public RsVisitor buildVisitor(@Nonnull RsProblemsHolder holder, boolean isOnTheFly) {
        return new RsWithMacrosInspectionVisitor() {
            @Override
            public void visitUnaryExpr(@Nonnull RsUnaryExpr expr) {
                if (isNegation(expr) && isNegation(expr.getExpr())) {
                    PsiElement minus = expr.getMinus();
                    List<LocalQuickFix> fixes = new ArrayList<>();
                    if (minus != null) {
                        LocalQuickFix fix = ReplaceIncDecOperatorFix.create(minus);
                        if (fix != null) {
                            fixes.add(fix);
                        }
                    }
                    holder.registerProblem(expr,
                        RsBundle.message("inspection.message.x.could.be.misinterpreted.as.pre.decrement.but.effectively.no.op"),
                        fixes.toArray(LocalQuickFix.EMPTY_ARRAY));
                }
            }
        };
    }

    @Override
    public boolean isSyntaxOnly() {
        return true;
    }

    private static boolean isNegation(RsExpr expr) {
        if (expr instanceof RsUnaryExpr) {
            return ((RsUnaryExpr) expr).getMinus() != null;
        }
        return false;
    }

    @Override
    @jakarta.annotation.Nonnull
    public consulo.localize.LocalizeValue getDisplayName() {
        return consulo.localize.LocalizeValue.of(org.rust.RsBundle.message("inspection.rs.double.neg.display.name"));
    }

    @Override
    @jakarta.annotation.Nonnull
    public consulo.localize.LocalizeValue getGroupDisplayName() {
        return consulo.localize.LocalizeValue.of(org.rust.RsBundle.message("rust"));
    }
}
