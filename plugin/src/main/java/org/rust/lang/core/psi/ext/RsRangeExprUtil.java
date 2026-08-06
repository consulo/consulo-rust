/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.psi.ext;

import consulo.language.psi.PsiElement;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.rust.lang.core.psi.RsExpr;
import org.rust.lang.core.psi.RsRangeExpr;

import java.util.List;

public final class RsRangeExprUtil {
    private RsRangeExprUtil() {
    }

    public static boolean isInclusive(@Nonnull RsRangeExpr rangeExpr) {
        return rangeExpr.getDotdotdot() != null || rangeExpr.getDotdoteq() != null;
    }

    public static boolean isExclusive(@Nonnull RsRangeExpr rangeExpr) {
        return rangeExpr.getDotdot() != null;
    }

    @Nullable
    public static PsiElement getOp(@Nonnull RsRangeExpr rangeExpr) {
        PsiElement op = rangeExpr.getDotdot();
        if (op != null) return op;
        op = rangeExpr.getDotdotdot();
        if (op != null) return op;
        return rangeExpr.getDotdoteq();
    }

    @Nullable
    public static RsExpr getStart(@Nonnull RsRangeExpr rangeExpr) {
        PsiElement op = getOp(rangeExpr);
        if (op == null) return null;
        List<RsExpr> exprList = rangeExpr.getExprList();
        if (exprList.isEmpty()) return null;
        RsExpr first = exprList.get(0);
        return first.getTextRange().getEndOffset() <= op.getTextRange().getStartOffset() ? first : null;
    }

    @Nullable
    public static RsExpr getEnd(@Nonnull RsRangeExpr rangeExpr) {
        PsiElement op = getOp(rangeExpr);
        if (op == null) return null;
        List<RsExpr> exprList = rangeExpr.getExprList();
        if (exprList.isEmpty()) return null;
        RsExpr last = exprList.get(exprList.size() - 1);
        return last.getTextRange().getStartOffset() >= op.getTextRange().getEndOffset() ? last : null;
    }
}
