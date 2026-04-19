/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.psi.ext;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.rust.lang.core.psi.RsArrayExpr;
import org.rust.lang.core.psi.RsExpr;

import java.util.List;

/**
 * Extension functions for {@link RsArrayExpr}.
 */
public final class RsArrayExprUtil {

    private RsArrayExprUtil() {
    }

    /**
     * Extracts the expression that defines the array initializer.
     */
    @Nullable
    public static RsExpr getInitializer(@NotNull RsArrayExpr expr) {
        if (expr.getSemicolon() != null && expr.getExprList().size() == 2) {
            return expr.getExprList().get(0);
        }
        return null;
    }

    /**
     * Extracts the expression that defines the size of an array.
     */
    @Nullable
    public static RsExpr getSizeExpr(@NotNull RsArrayExpr expr) {
        if (expr.getSemicolon() != null && expr.getExprList().size() == 2) {
            return expr.getExprList().get(1);
        }
        return null;
    }

    /**
     * Extracts the expression list that defines the elements of an array.
     */
    @Nullable
    public static List<RsExpr> getArrayElements(@NotNull RsArrayExpr expr) {
        if (expr.getSemicolon() == null) {
            return expr.getExprList();
        }
        return null;
    }
}
