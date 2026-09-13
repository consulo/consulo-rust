/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.psi.ext.impl;

import consulo.language.psi.PsiElement;
import jakarta.annotation.Nonnull;
import org.rust.lang.core.psi.RsBinaryExpr;
import org.rust.lang.core.psi.ext.*;

/**
 * Extension functions for {@link RsBinaryExpr}.
 */
public final class RsBinaryExprUtil {

    private RsBinaryExprUtil() {
    }

    @Nonnull
    public static PsiElement getOperator(@Nonnull RsBinaryExpr expr) {
        return RsBinaryOpImplUtil.getOperator(expr.getBinaryOp());
    }

    @Nonnull
    public static BinaryOperator getOperatorType(@Nonnull RsBinaryExpr expr) {
        return RsBinaryOpImplUtil.getOperatorType(expr.getBinaryOp());
    }

    public static boolean isAssignBinaryExpr(@Nonnull RsBinaryExpr expr) {
        return getOperatorType(expr) instanceof AssignmentOp;
    }
}
