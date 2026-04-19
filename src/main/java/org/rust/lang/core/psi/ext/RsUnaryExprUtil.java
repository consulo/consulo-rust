/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.psi.ext;

import com.intellij.psi.StubBasedPsiElement;
import com.intellij.psi.stubs.StubElement;
import org.jetbrains.annotations.NotNull;
import org.rust.lang.core.psi.RsUnaryExpr;
import org.rust.lang.core.stubs.RsUnaryExprStub;

/**
 * Delegates to {@link RsExprUtil} and {@link RsUnaryExprExtKt} for the actual implementations.
 */
public final class RsUnaryExprUtil {

    private RsUnaryExprUtil() {
    }

    /**
     * Returns the operator type of a unary expression.
     */
    @NotNull
    public static UnaryOperator getOperatorType(@NotNull RsUnaryExpr expr) {
        if (expr instanceof StubBasedPsiElement) {
            StubElement<?> stub = ((StubBasedPsiElement<?>) expr).getStub();
            if (stub instanceof RsUnaryExprStub) {
                return ((RsUnaryExprStub) stub).getOperatorType();
            }
        }
        if (RsUnaryExprExtUtil.getRaw(expr) != null) {
            if (expr.getConst() != null) return UnaryOperator.RAW_REF_CONST;
            if (expr.getMut() != null) return UnaryOperator.RAW_REF_MUT;
            throw new IllegalStateException("Unknown unary operator type: `" + expr.getText() + "`");
        }
        if (expr.getMut() != null) return UnaryOperator.REF_MUT;
        if (expr.getAnd() != null) return UnaryOperator.REF;
        if (expr.getMul() != null) return UnaryOperator.DEREF;
        if (expr.getMinus() != null) return UnaryOperator.MINUS;
        if (expr.getExcl() != null) return UnaryOperator.NOT;
        if (expr.getBox() != null) return UnaryOperator.BOX;
        throw new IllegalStateException("Unknown unary operator type: `" + expr.getText() + "`");
    }

    /**
     * Returns true if this unary expression is a dereference ({@code *expr}).
     */
    public static boolean isDereference(@NotNull RsUnaryExpr expr) {
        return RsUnaryExprExtUtil.isDereference(expr);
    }
}
