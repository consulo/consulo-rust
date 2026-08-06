/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.psi.ext;

import consulo.language.psi.PsiElement;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.rust.lang.core.psi.*;
import org.rust.lang.core.types.ExtensionsUtil;
import org.rust.lang.core.types.ty.Ty;

/**
 * Delegates to the specific utility classes for the actual implementations.
 */
public final class RsExprUtil {

    private RsExprUtil() {
    }

    @Nonnull
    public static UnaryOperator getOperatorType(@Nonnull RsUnaryExpr expr) {
        return RsUnaryExprUtil.getOperatorType(expr);
    }

    @Nonnull
    public static BinaryOperator getOperatorType(@Nonnull RsBinaryExpr expr) {
        return RsBinaryExprUtil.getOperatorType(expr);
    }

    @Nonnull
    public static BinaryOperator getOperatorType(@Nonnull RsBinaryOp binaryOp) {
        return RsBinaryOpUtil.getOperatorType(binaryOp);
    }

    @Nonnull
    public static PsiElement getOperator(@Nonnull RsBinaryExpr expr) {
        return RsBinaryExprUtil.getOperator(expr);
    }

    @Nonnull
    public static RsExpr unwrapReference(@Nonnull RsExpr expr) {
        return RsPsiJavaUtil.unwrapReference(expr);
    }

    @Nonnull
    public static RsExpr unwrapParenExprs(@Nonnull RsExpr expr) {
        RsExpr current = expr;
        while (current instanceof RsParenExpr) {
            RsExpr inner = ((RsParenExpr) current).getExpr();
            if (inner == null) break;
            current = inner;
        }
        return current;
    }

    public static boolean isAssignBinaryExpr(@Nonnull RsExpr expr) {
        if (!(expr instanceof RsBinaryExpr)) return false;
        return RsBinaryExprUtil.isAssignBinaryExpr((RsBinaryExpr) expr);
    }

    public static boolean isTailExpr(@Nonnull RsExpr expr) {
        return RsExprExtUtil.isTailExpr(expr);
    }

    public static boolean getHasSideEffects(@Nonnull RsExpr expr) {
        // Default implementation: expressions may have side effects
        return true;
    }

    public static boolean isInConstContext(@Nonnull RsExpr expr) {
        return RsExprExtUtil.isInConstContext(expr);
    }

    @Nullable
    public static RsConstContextKind getClassifyConstContext(@Nonnull RsExpr expr) {
        // Stub implementation - returns null when not in const context
        return null;
    }

    public static boolean isInUnsafeContext(@Nonnull RsExpr expr) {
        return RsExprExtUtil.isInUnsafeContext(expr);
    }

    @Nonnull
    public static Ty getType(@Nonnull RsExpr expr) {
        return ExtensionsUtil.getType(expr);
    }

    @Nonnull
    public static RsElement replaceWithExpr(@Nonnull RsMacroCall macroCall, @Nonnull RsExpr expr) {
        return RsMacroCallUtil.replaceWithExpr(macroCall, expr);
    }

    public static void processBreakExprs(@Nonnull RsLabeledExpression labeledExpr,
                                          @Nullable String label,
                                          boolean matchOnlyByLabel,
                                          @Nonnull java.util.function.Consumer<RsBreakExpr> sink) {
        RsLabeledExpressionUtil.processBreakExprs(labeledExpr, label, matchOnlyByLabel, sink);
    }
}
