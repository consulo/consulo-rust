/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.psi.ext.impl;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.rust.lang.core.psi.RsExpr;
import org.rust.lang.core.psi.RsIndexExpr;
import org.rust.lang.core.psi.ext.*;

/**
 * Delegates to {@link RsIndexExprExtKt} for the actual implementations.
 */
public final class RsIndexExprUtil {

    private RsIndexExprUtil() {
    }

    @Nonnull
    public static RsExpr getContainerExpr(@Nonnull RsIndexExpr indexExpr) {
        return RsIndexExprExtUtil.getContainerExpr(indexExpr);
    }

    @Nullable
    public static RsExpr getIndexExpr(@Nonnull RsIndexExpr indexExpr) {
        return RsIndexExprExtUtil.getIndexExpr(indexExpr);
    }
}
