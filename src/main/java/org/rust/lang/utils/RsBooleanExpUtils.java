/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.utils;

import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;
import org.rust.lang.core.psi.*;
import org.rust.lang.core.psi.ext.ComparisonOp;
import org.rust.lang.core.psi.ext.EqualityOp;
import org.rust.lang.core.psi.ext.RsBinaryExprUtil;

public final class RsBooleanExpUtils {
    private RsBooleanExpUtils() {
    }

    @NotNull
    public static String negateToString(@NotNull RsBinaryExpr expr) {
        String lhs = expr.getLeft().getText();
        RsExpr right = expr.getRight();
        String rhs = right != null ? right.getText() : "";
        Object operatorType = RsBinaryExprUtil.getOperatorType(expr);
        String op;
        if (operatorType == EqualityOp.EQ) {
            op = "!=";
        } else if (operatorType == EqualityOp.EXCLEQ) {
            op = "==";
        } else if (operatorType == ComparisonOp.GT) {
            op = "<=";
        } else if (operatorType == ComparisonOp.LT) {
            op = ">=";
        } else if (operatorType == ComparisonOp.GTEQ) {
            op = "<";
        } else if (operatorType == ComparisonOp.LTEQ) {
            op = ">";
        } else {
            op = null;
        }
        if (op != null) {
            return lhs + " " + op + " " + rhs;
        } else {
            return "!(" + expr.getText() + ")";
        }
    }

    public static boolean isNegation(@NotNull PsiElement element) {
        return element instanceof RsUnaryExpr && ((RsUnaryExpr) element).getExcl() != null;
    }

    @NotNull
    public static PsiElement negate(@NotNull PsiElement element) {
        RsPsiFactory psiFactory = new RsPsiFactory(element.getProject(), true, false);
        if (isNegation(element)) {
            RsExpr inner = ((RsUnaryExpr) element).getExpr();
            if (inner == null) throw new IllegalStateException("Expected non-null inner expression");
            if (inner instanceof RsParenExpr) {
                RsExpr parenInner = ((RsParenExpr) inner).getExpr();
                if (parenInner != null) return parenInner;
            }
            return inner;
        }

        if (element instanceof RsBinaryExpr) {
            return psiFactory.createExpression(negateToString((RsBinaryExpr) element));
        }

        if (element instanceof RsParenExpr || element instanceof RsPathExpr || element instanceof RsCallExpr) {
            return psiFactory.createExpression("!" + element.getText());
        }

        if (element instanceof RsLitExpr) {
            RsLitExpr litExpr = (RsLitExpr) element;
            PsiElement boolLiteral = litExpr.getBoolLiteral();
            if (boolLiteral != null) {
                String text = boolLiteral.getText();
                if ("false".equals(text)) {
                    return psiFactory.createExpression("true");
                } else if ("true".equals(text)) {
                    return psiFactory.createExpression("false");
                } else {
                    throw new IllegalStateException("unreachable");
                }
            }
        }

        return psiFactory.createExpression("!(" + element.getText() + ")");
    }
}
