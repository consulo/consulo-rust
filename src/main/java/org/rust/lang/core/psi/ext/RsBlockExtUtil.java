/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.psi.ext;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.rust.lang.core.psi.RsBlock;
import org.rust.lang.core.psi.RsExpr;

/**
 * Bridge class delegating to {@link RsBlockKt}.
 * Some converted Java code references RsBlockExtUtil instead of RsBlockUtil.
 */
public final class RsBlockExtUtil {

    private RsBlockExtUtil() {
    }

    @NotNull
    public static RsBlockUtil.ExpandedStmtsAndTailExpr getExpandedStmtsAndTailExpr(@NotNull RsBlock block) {
        return RsBlockUtil.getExpandedStmtsAndTailExpr(block);
    }

    @NotNull
    public static RsBlockUtil.ExpandedStmtsAndTailExpr expandedStmtsAndTailExpr(@NotNull RsBlock block) {
        return RsBlockUtil.getExpandedStmtsAndTailExpr(block);
    }

    @Nullable
    public static RsExpr getExpandedTailExpr(@NotNull RsBlock block) {
        return RsBlockUtil.getExpandedTailExpr(block);
    }
}
