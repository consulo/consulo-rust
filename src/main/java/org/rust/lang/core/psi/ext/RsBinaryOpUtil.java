/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.psi.ext;

import org.jetbrains.annotations.NotNull;
import org.rust.lang.core.psi.RsBinaryExpr;
import org.rust.lang.core.psi.RsBinaryOp;

/**
 * Delegates to {@link RsExprUtil} for the actual implementations.
 */
public final class RsBinaryOpUtil {

    private RsBinaryOpUtil() {
    }

    @NotNull
    public static BinaryOperator getOperatorType(@NotNull RsBinaryExpr expr) {
        return RsBinaryExprUtil.getOperatorType(expr);
    }

    @NotNull
    public static BinaryOperator getOperatorType(@NotNull RsBinaryOp binaryOp) {
        return RsBinaryOpImplUtil.getOperatorType(binaryOp);
    }
}
