/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.inspections;

import consulo.document.util.TextRange;
import consulo.language.psi.PsiElement;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.rust.RsBundle;
import org.rust.ide.fixes.SubstituteTextFix;
import org.rust.lang.core.psi.RsBinaryExpr;
import org.rust.lang.core.psi.RsExpr;
import org.rust.lang.core.psi.RsUnaryExpr;
import org.rust.lang.core.psi.RsVisitor;
import org.rust.lang.core.psi.ext.impl.RsBinaryExprUtil;
import consulo.annotation.component.ExtensionImpl;
import consulo.localize.LocalizeValue;

/**
 * Checks for use of the non-existent =*, =! and =- operators that are probably typos but can be compiled.
 * Analogue of Clippy's suspicious_assignment_formatting.
 * QuickFix 1: Change {@code a =? b} to {@code a ?= b}
 * QuickFix 2: Change {@code a =? b} to {@code a = ?b}
 */
@ExtensionImpl
public class RsSuspiciousAssignmentInspection extends RsLocalInspectionTool {

    private static final int LONG_TEXT_THRESHOLD = 10;
    private static final String LONG_TEXT_SUBST = "..";

@Override
    public RsVisitor buildVisitor(@Nonnull RsProblemsHolder holder, boolean isOnTheFly) {
        return new RsWithMacrosInspectionVisitor() {
            @Override
            public void visitBinaryExpr(@Nonnull RsBinaryExpr expr) {
                if (!RsBinaryExprUtil.getOperator(expr).getText().equals("=")) return;
                RsUnaryExpr unaryExpr = findUnaryExpr(expr.getRight());
                if (unaryExpr == null || unaryExpr.getExpr() == null) return;
                RsExpr unaryBody = unaryExpr.getExpr();
                char op = unaryExpr.getText().charAt(0);
                if (unaryBody != null
                    && (op == '-' || op == '*' || op == '!')
                    && distanceTo(RsBinaryExprUtil.getOperator(expr), unaryExpr) == 1
                    && distanceTo(RsBinaryExprUtil.getOperator(expr), unaryBody) > 2) {
                    int uExprOffset = unaryBody.getTextOffset() - expr.getLeft().getTextOffset();
                    String left = compact(expr.getLeft().getText());
                    String right = compact(expr.getText().substring(uExprOffset));
                    String right2 = right.equals(LONG_TEXT_SUBST) ? "(" + op + right + ")" : op + right;
                    String subst1 = left + " " + op + "= " + right;
                    String subst2 = left + " = " + right2;
                    TextRange substRange = new TextRange(expr.getLeft().getTextOffset() + expr.getLeft().getTextLength(), unaryBody.getTextOffset());
                    holder.registerProblem(
                        expr,
                        new TextRange(expr.getLeft().getText().length(), uExprOffset),
                        RsBundle.message("inspection.message.suspicious.assignment.did.you.mean.or", subst1, subst2),
                        SubstituteTextFix.replace(RsBundle.message("intention.name.change.to3", subst1), expr.getContainingFile(), substRange, " " + op + "= "),
                        SubstituteTextFix.replace(RsBundle.message("intention.name.change.to2", subst2), expr.getContainingFile(), substRange, " = " + op)
                    );
                }
            }
        };
    }

    @Override
    public boolean isSyntaxOnly() {
        return true;
    }

    private static int distanceTo(@Nonnull PsiElement from, @Nonnull PsiElement to) {
        return to.getTextOffset() - from.getTextOffset();
    }

    @Nonnull
    private static String compact(@Nonnull String text) {
        return text.length() <= LONG_TEXT_THRESHOLD ? text : LONG_TEXT_SUBST;
    }

    @Nullable
    private static RsUnaryExpr findUnaryExpr(@Nullable RsExpr el) {
        if (el instanceof RsUnaryExpr) {
            return (RsUnaryExpr) el;
        } else if (el instanceof RsBinaryExpr) {
            return findUnaryExpr(((RsBinaryExpr) el).getLeft());
        }
        return null;
    }

    @Override
    @jakarta.annotation.Nonnull
    public consulo.localize.LocalizeValue getDisplayName() {
        return consulo.localize.LocalizeValue.of(org.rust.RsBundle.message("inspection.rs.suspicious.assignment.display.name"));
    }

    @Override
    @jakarta.annotation.Nonnull
    public consulo.localize.LocalizeValue getGroupDisplayName() {
        return consulo.localize.LocalizeValue.of(org.rust.RsBundle.message("rust"));
    }
}
