/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.psi.ext;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.rust.lang.core.psi.RsBlock;
import org.rust.lang.core.psi.RsExpr;

/**
 * Bridge class delegating to {@link RsBlockUtil}.
 */
public final class RsBlockExtUtil {

    private RsBlockExtUtil() {
    }

    @Nonnull
    public static RsBlockUtil.ExpandedStmtsAndTailExpr getExpandedStmtsAndTailExpr(@Nonnull RsBlock block) {
        return RsBlockUtil.getExpandedStmtsAndTailExpr(block);
    }

    @Nonnull
    public static RsBlockUtil.ExpandedStmtsAndTailExpr expandedStmtsAndTailExpr(@Nonnull RsBlock block) {
        return RsBlockUtil.getExpandedStmtsAndTailExpr(block);
    }

    @Nullable
    public static RsExpr getExpandedTailExpr(@Nonnull RsBlock block) {
        return RsBlockUtil.getExpandedTailExpr(block);
    }
}
