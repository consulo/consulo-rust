/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.psi.ext;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.rust.lang.core.psi.RsExpr;
import org.rust.lang.core.psi.RsIndexExpr;

import java.util.List;

public final class RsIndexExprExtUtil {
    private RsIndexExprExtUtil() {
    }

    @NotNull
    public static RsExpr getContainerExpr(@NotNull RsIndexExpr indexExpr) {
        return indexExpr.getExprList().get(0);
    }

    @Nullable
    public static RsExpr getIndexExpr(@NotNull RsIndexExpr indexExpr) {
        List<RsExpr> list = indexExpr.getExprList();
        return list.size() > 1 ? list.get(1) : null;
    }
}
