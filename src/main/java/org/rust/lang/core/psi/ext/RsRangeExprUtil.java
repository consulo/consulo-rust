/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.psi.ext;

import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.rust.lang.core.psi.RsExpr;
import org.rust.lang.core.psi.RsRangeExpr;

import java.util.List;

public final class RsRangeExprUtil {
    private RsRangeExprUtil() {
    }

    public static boolean isInclusive(@NotNull RsRangeExpr rangeExpr) {
        return rangeExpr.getDotdotdot() != null || rangeExpr.getDotdoteq() != null;
    }

    public static boolean isExclusive(@NotNull RsRangeExpr rangeExpr) {
        return rangeExpr.getDotdot() != null;
    }

    @Nullable
    public static PsiElement getOp(@NotNull RsRangeExpr rangeExpr) {
        PsiElement op = rangeExpr.getDotdot();
        if (op != null) return op;
        op = rangeExpr.getDotdotdot();
        if (op != null) return op;
        return rangeExpr.getDotdoteq();
    }

    @Nullable
    public static RsExpr getStart(@NotNull RsRangeExpr rangeExpr) {
        PsiElement op = getOp(rangeExpr);
        if (op == null) return null;
        List<RsExpr> exprList = rangeExpr.getExprList();
        if (exprList.isEmpty()) return null;
        RsExpr first = exprList.get(0);
        return first.getTextRange().getEndOffset() <= op.getTextRange().getStartOffset() ? first : null;
    }

    @Nullable
    public static RsExpr getEnd(@NotNull RsRangeExpr rangeExpr) {
        PsiElement op = getOp(rangeExpr);
        if (op == null) return null;
        List<RsExpr> exprList = rangeExpr.getExprList();
        if (exprList.isEmpty()) return null;
        RsExpr last = exprList.get(exprList.size() - 1);
        return last.getTextRange().getStartOffset() >= op.getTextRange().getEndOffset() ? last : null;
    }
}
