/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.psi.ext;

import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.rust.lang.core.psi.RsPatConst;
import org.rust.lang.core.psi.RsPatRange;

import java.util.List;

public final class RsPatRangeUtil {
    private RsPatRangeUtil() {
    }

    public static boolean isInclusive(@NotNull RsPatRange range) {
        return range.getDotdotdot() != null || range.getDotdoteq() != null;
    }

    public static boolean isExclusive(@NotNull RsPatRange range) {
        return range.getDotdot() != null;
    }

    @Nullable
    public static PsiElement getOp(@NotNull RsPatRange range) {
        PsiElement op = range.getDotdot();
        if (op != null) return op;
        op = range.getDotdotdot();
        if (op != null) return op;
        return range.getDotdoteq();
    }

    @Nullable
    public static RsPatConst getStart(@NotNull RsPatRange range) {
        PsiElement op = getOp(range);
        if (op == null) return null;
        List<RsPatConst> list = range.getPatConstList();
        if (list.isEmpty()) return null;
        RsPatConst first = list.get(0);
        return first.getTextRange().getEndOffset() <= op.getTextRange().getStartOffset() ? first : null;
    }

    @Nullable
    public static RsPatConst getEnd(@NotNull RsPatRange range) {
        PsiElement op = getOp(range);
        if (op == null) return null;
        List<RsPatConst> list = range.getPatConstList();
        if (list.isEmpty()) return null;
        RsPatConst last = list.get(list.size() - 1);
        return last.getTextRange().getStartOffset() >= op.getTextRange().getEndOffset() ? last : null;
    }
}
