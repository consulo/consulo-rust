/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.psi.ext;

import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;
import org.rust.lang.core.psi.RsBinaryExpr;

/**
 * Extension functions for {@link RsBinaryExpr}.
 */
public final class RsBinaryExprUtil {

    private RsBinaryExprUtil() {
    }

    @NotNull
    public static PsiElement getOperator(@NotNull RsBinaryExpr expr) {
        return RsBinaryOpImplUtil.getOperator(expr.getBinaryOp());
    }

    @NotNull
    public static BinaryOperator getOperatorType(@NotNull RsBinaryExpr expr) {
        return RsBinaryOpImplUtil.getOperatorType(expr.getBinaryOp());
    }

    public static boolean isAssignBinaryExpr(@NotNull RsBinaryExpr expr) {
        return getOperatorType(expr) instanceof AssignmentOp;
    }
}
