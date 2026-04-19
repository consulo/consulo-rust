/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.psi.ext;

import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.rust.lang.core.psi.*;
import org.rust.lang.core.types.ExtensionsUtil;
import org.rust.lang.core.types.ty.Ty;

/**
 * Delegates to the specific utility classes for the actual implementations.
 */
public final class RsExprUtil {

    private RsExprUtil() {
    }

    @NotNull
    public static UnaryOperator getOperatorType(@NotNull RsUnaryExpr expr) {
        return RsUnaryExprUtil.getOperatorType(expr);
    }

    @NotNull
    public static BinaryOperator getOperatorType(@NotNull RsBinaryExpr expr) {
        return RsBinaryExprUtil.getOperatorType(expr);
    }

    @NotNull
    public static BinaryOperator getOperatorType(@NotNull RsBinaryOp binaryOp) {
        return RsBinaryOpUtil.getOperatorType(binaryOp);
    }

    @NotNull
    public static PsiElement getOperator(@NotNull RsBinaryExpr expr) {
        return RsBinaryExprUtil.getOperator(expr);
    }

    @NotNull
    public static RsExpr unwrapReference(@NotNull RsExpr expr) {
        return RsPsiJavaUtil.unwrapReference(expr);
    }

    @NotNull
    public static RsExpr unwrapParenExprs(@NotNull RsExpr expr) {
        RsExpr current = expr;
        while (current instanceof RsParenExpr) {
            RsExpr inner = ((RsParenExpr) current).getExpr();
            if (inner == null) break;
            current = inner;
        }
        return current;
    }

    public static boolean isAssignBinaryExpr(@NotNull RsExpr expr) {
        if (!(expr instanceof RsBinaryExpr)) return false;
        return RsBinaryExprUtil.isAssignBinaryExpr((RsBinaryExpr) expr);
    }

    public static boolean isTailExpr(@NotNull RsExpr expr) {
        return RsExprExtUtil.isTailExpr(expr);
    }

    public static boolean getHasSideEffects(@NotNull RsExpr expr) {
        // Default implementation: expressions may have side effects
        return true;
    }

    public static boolean isInConstContext(@NotNull RsExpr expr) {
        return RsExprExtUtil.isInConstContext(expr);
    }

    @Nullable
    public static RsConstContextKind getClassifyConstContext(@NotNull RsExpr expr) {
        // Stub implementation - returns null when not in const context
        return null;
    }

    public static boolean isInUnsafeContext(@NotNull RsExpr expr) {
        return RsExprExtUtil.isInUnsafeContext(expr);
    }

    @NotNull
    public static Ty getType(@NotNull RsExpr expr) {
        return ExtensionsUtil.getType(expr);
    }

    @NotNull
    public static RsElement replaceWithExpr(@NotNull RsMacroCall macroCall, @NotNull RsExpr expr) {
        return RsMacroCallUtil.replaceWithExpr(macroCall, expr);
    }

    public static void processBreakExprs(@NotNull RsLabeledExpression labeledExpr,
                                          @Nullable String label,
                                          boolean matchOnlyByLabel,
                                          @NotNull java.util.function.Consumer<RsBreakExpr> sink) {
        RsLabeledExpressionUtil.processBreakExprs(labeledExpr, label, matchOnlyByLabel, sink);
    }
}
