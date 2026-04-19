/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.refactoring.inlineValue;

import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;
import org.rust.lang.core.psi.*;
import org.rust.lang.core.psi.ext.RsElement;

/**
 * Utility methods for inline value refactoring.
 */
public final class InlineValueUtils {

    private InlineValueUtils() {
    }

    /**
     * Replaces the element either with {@code expr} or {@code (expr)}, depending on context.
     */
    @NotNull
    public static PsiElement replaceWithAddingParentheses(
        @NotNull RsElement element,
        @NotNull RsElement expr,
        @NotNull RsPsiFactory factory
    ) {
        PsiElement parent = element.getParent();
        boolean needsParentheses = false;

        if (expr instanceof RsBinaryExpr && (parent instanceof RsBinaryExpr || requiresSingleExpr(parent))) {
            needsParentheses = true;
        } else if (isBlockLikeExpr(expr) && requiresSingleExpr(parent)) {
            needsParentheses = true;
        } else if (expr instanceof RsStructLiteral &&
            (parent instanceof RsMatchExpr || parent instanceof RsForExpr ||
                parent instanceof RsCondition || parent instanceof RsLetExpr)) {
            needsParentheses = true;
        }

        PsiElement newExpr;
        if (needsParentheses) {
            newExpr = factory.createExpression("(" + expr.getText() + ")");
        } else {
            newExpr = expr;
        }
        return element.replace(newExpr);
    }

    private static boolean isBlockLikeExpr(@NotNull PsiElement element) {
        return element instanceof RsRangeExpr || element instanceof RsLambdaExpr ||
            element instanceof RsMatchExpr || element instanceof RsBlockExpr ||
            element instanceof RsLoopExpr || element instanceof RsWhileExpr;
    }

    private static boolean requiresSingleExpr(@NotNull PsiElement element) {
        return element instanceof RsDotExpr || element instanceof RsTryExpr ||
            element instanceof RsUnaryExpr || element instanceof RsCastExpr ||
            element instanceof RsCallExpr;
    }
}
