/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.psi.ext.impl;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.rust.lang.core.psi.RsExpr;
import org.rust.lang.core.psi.RsIndexExpr;

import java.util.List;
import org.rust.lang.core.psi.ext.*;

public final class RsIndexExprExtUtil {
    private RsIndexExprExtUtil() {
    }

    @Nonnull
    public static RsExpr getContainerExpr(@Nonnull RsIndexExpr indexExpr) {
        return indexExpr.getExprList().get(0);
    }

    @Nullable
    public static RsExpr getIndexExpr(@Nonnull RsIndexExpr indexExpr) {
        List<RsExpr> list = indexExpr.getExprList();
        return list.size() > 1 ? list.get(1) : null;
    }
}
