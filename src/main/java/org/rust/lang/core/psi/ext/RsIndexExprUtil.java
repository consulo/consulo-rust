/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.psi.ext;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.rust.lang.core.psi.RsExpr;
import org.rust.lang.core.psi.RsIndexExpr;

/**
 * Delegates to {@link RsIndexExprExtKt} for the actual implementations.
 */
public final class RsIndexExprUtil {

    private RsIndexExprUtil() {
    }

    @NotNull
    public static RsExpr getContainerExpr(@NotNull RsIndexExpr indexExpr) {
        return RsIndexExprExtUtil.getContainerExpr(indexExpr);
    }

    @Nullable
    public static RsExpr getIndexExpr(@NotNull RsIndexExpr indexExpr) {
        return RsIndexExprExtUtil.getIndexExpr(indexExpr);
    }
}
