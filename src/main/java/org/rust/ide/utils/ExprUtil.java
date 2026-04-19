/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.utils;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.rust.lang.core.psi.*;
import org.rust.lang.core.psi.ext.LogicOp;
import org.rust.lang.core.psi.ext.RsBinaryExprUtil;
import org.rust.lang.core.psi.ext.RsExprUtil;

/**
 * Utilities for working with Rust expressions.
 */
public final class ExprUtil {

    private ExprUtil() {
    }

    /**
     * Check if an expression is functionally pure
     * (has no side-effects and throws no errors).
     *
     * @return {@code true} if the expression is pure, {@code false} if
     *         it is not pure (has side-effects / throws errors)
     *         or {@code null} if it is unknown.
     */
    @Nullable
    public static Boolean isPure(@NotNull RsExpr expr) {
        if (expr instanceof RsArrayExpr) {
            RsArrayExpr arrayExpr = (RsArrayExpr) expr;
            if (arrayExpr.getSemicolon() == null) {
                return allMaybe(arrayExpr.getExprList());
            } else {
                return isPure(arrayExpr.getExprList().get(0));
            }
        }
        if (expr instanceof RsStructLiteral) {
            RsStructLiteral structLiteral = (RsStructLiteral) expr;
            if (structLiteral.getStructLiteralBody().getDotdot() == null) {
                for (RsStructLiteralField field : structLiteral.getStructLiteralBody().getStructLiteralFieldList()) {
                    RsExpr fieldExpr = field.getExpr();
                    if (fieldExpr != null) {
                        Boolean result = isPure(fieldExpr);
                        if (result == null || !result) return result;
                    }
                }
                return true;
            } else {
                return null;
            }
        }
        if (expr instanceof RsBinaryExpr) {
            RsBinaryExpr binaryExpr = (RsBinaryExpr) expr;
            if (RsBinaryExprUtil.getOperatorType(binaryExpr) instanceof LogicOp) {
                RsExpr left = binaryExpr.getLeft();
                RsExpr right = binaryExpr.getRight();
                Boolean leftPure = isPure(left);
                Boolean rightPure = right != null ? isPure(right) : null;
                if (leftPure == null || rightPure == null) return null;
                return leftPure && rightPure;
            }
            return null;
        }
        if (expr instanceof RsTupleExpr) {
            return allMaybe(((RsTupleExpr) expr).getExprList());
        }
        if (expr instanceof RsDotExpr) {
            RsDotExpr dotExpr = (RsDotExpr) expr;
            if (dotExpr.getMethodCall() != null) return null;
            return isPure(dotExpr.getExpr());
        }
        if (expr instanceof RsParenExpr) {
            RsExpr inner = ((RsParenExpr) expr).getExpr();
            return inner != null ? isPure(inner) : null;
        }
        if (expr instanceof RsBreakExpr || expr instanceof RsContExpr
            || expr instanceof RsRetExpr || expr instanceof RsTryExpr) {
            return false;
        }
        if (expr instanceof RsPathExpr || expr instanceof RsLitExpr || expr instanceof RsUnitExpr) {
            return true;
        }
        return null;
    }

    /**
     * Go to the RsExpr, which parent is not RsParenExpr.
     */
    @NotNull
    public static RsExpr skipParenExprUp(@NotNull RsExpr expr) {
        RsExpr element = expr;
        while (element.getParent() instanceof RsParenExpr) {
            element = (RsParenExpr) element.getParent();
        }
        return element;
    }

    /**
     * Go down to the item below RsParenExpr.
     */
    @Nullable
    public static RsExpr skipParenExprDown(@NotNull RsCondition condition) {
        RsExpr expr = condition.getExpr();
        if (expr == null) return null;
        return RsExprUtil.unwrapParenExprs(expr);
    }

    @Nullable
    private static Boolean allMaybe(@NotNull java.util.List<RsExpr> exprs) {
        boolean allTrueWithNulls = true;
        boolean allTrueWithFalse = true;
        for (RsExpr e : exprs) {
            Boolean result = isPure(e);
            if (result == null) {
                allTrueWithFalse = false;
            } else if (!result) {
                allTrueWithNulls = false;
                allTrueWithFalse = false;
            }
        }
        if (allTrueWithNulls == allTrueWithFalse) return allTrueWithNulls;
        return null;
    }
}
