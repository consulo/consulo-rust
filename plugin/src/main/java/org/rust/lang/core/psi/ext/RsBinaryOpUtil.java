/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.psi.ext;

import jakarta.annotation.Nonnull;
import org.rust.lang.core.psi.RsBinaryExpr;
import org.rust.lang.core.psi.RsBinaryOp;

/**
 * Delegates to {@link RsExprUtil} for the actual implementations.
 */
public final class RsBinaryOpUtil {

    private RsBinaryOpUtil() {
    }

    @Nonnull
    public static BinaryOperator getOperatorType(@Nonnull RsBinaryExpr expr) {
        return RsBinaryExprUtil.getOperatorType(expr);
    }

    @Nonnull
    public static BinaryOperator getOperatorType(@Nonnull RsBinaryOp binaryOp) {
        return RsBinaryOpImplUtil.getOperatorType(binaryOp);
    }
}
