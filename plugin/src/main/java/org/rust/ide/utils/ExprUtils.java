/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.utils;

import org.rust.lang.core.psi.*;
import org.rust.lang.core.psi.ext.LogicOp;
import org.rust.lang.core.psi.ext.RsBinaryExprUtil;
import org.rust.lang.core.psi.ext.RsExprUtil;

import java.util.List;

public final class ExprUtils {
    private ExprUtils() {
    }

    /**
     * Check if an expression is functionally pure
     * (has no side-effects and throws no errors).
     *
     * @return {@code true} if the expression is pure, {@code false} if
     *         it is not pure (has side-effects / throws errors)
     *         or {@code null} if it is unknown.
     */
    public static Boolean isPure(RsExpr expr) {
        if (expr instanceof RsArrayExpr) {
            RsArrayExpr arrayExpr = (RsArrayExpr) expr;
            if (arrayExpr.getSemicolon() == null) {
                return allMaybe(arrayExpr.getExprList());
            } else {
                return isPure(arrayExpr.getExprList().get(0));
            }
        } else if (expr instanceof RsStructLiteral) {
            RsStructLiteral structLiteral = (RsStructLiteral) expr;
            if (structLiteral.getStructLiteralBody().getDotdot() == null) {
                List<RsStructLiteralField> fields = structLiteral.getStructLiteralBody().getStructLiteralFieldList();
                for (RsStructLiteralField field : fields) {
                    RsExpr fieldExpr = field.getExpr();
                    if (fieldExpr != null) {
                        Boolean result = isPure(fieldExpr);
                        if (result == null) return null;
                        if (!result) return false;
                    }
                }
                return true;
            } else {
                return null;
            }
        } else if (expr instanceof RsBinaryExpr) {
            RsBinaryExpr binExpr = (RsBinaryExpr) expr;
            if (RsBinaryExprUtil.getOperatorType(binExpr) instanceof LogicOp) {
                Boolean leftPure = isPure(binExpr.getLeft());
                RsExpr right = binExpr.getRight();
                if (right == null) return leftPure;
                Boolean rightPure = isPure(right);
                if (leftPure == null || rightPure == null) return null;
                return leftPure && rightPure;
            }
            return null;
        } else if (expr instanceof RsTupleExpr) {
            return allMaybe(((RsTupleExpr) expr).getExprList());
        } else if (expr instanceof RsDotExpr) {
            RsDotExpr dotExpr = (RsDotExpr) expr;
            if (dotExpr.getMethodCall() != null) return null;
            return isPure(dotExpr.getExpr());
        } else if (expr instanceof RsParenExpr) {
            RsExpr inner = ((RsParenExpr) expr).getExpr();
            if (inner == null) return null;
            return isPure(inner) == Boolean.TRUE;
        } else if (expr instanceof RsBreakExpr || expr instanceof RsContExpr
            || expr instanceof RsRetExpr || expr instanceof RsTryExpr) {
            return false;
        } else if (expr instanceof RsPathExpr || expr instanceof RsLitExpr || expr instanceof RsUnitExpr) {
            return true;
        } else {
            return null;
        }
    }

    private static Boolean allMaybe(List<? extends RsExpr> exprs) {
        boolean hasNull = false;
        for (RsExpr e : exprs) {
            Boolean result = isPure(e);
            if (result == null) {
                hasNull = true;
            } else if (!result) {
                return false;
            }
        }
        return hasNull ? null : true;
    }

    /**
     * Go to the RsExpr, which parent is not RsParenExpr.
     *
     * @return RsExpr, which parent is not RsParenExpr.
     */
    public static RsExpr skipParenExprUp(RsExpr expr) {
        RsExpr element = expr;
        while (element.getParent() instanceof RsParenExpr) {
            element = (RsParenExpr) element.getParent();
        }
        return element;
    }

    /**
     * Go down to the item below RsParenExpr.
     *
     * @return a child expression without parentheses.
     */
    public static RsExpr skipParenExprDown(RsCondition condition) {
        RsExpr expr = condition.getExpr();
        if (expr == null) return null;
        return RsExprUtil.unwrapParenExprs(expr);
    }
}
