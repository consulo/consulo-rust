/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.psi;

import org.jetbrains.annotations.NotNull;
import org.rust.lang.core.psi.ext.RsElement;
import org.rust.lang.core.psi.ext.PsiElementUtil;

import java.util.List;

public final class PsiModificationUtils {

    public static void ensureTrailingComma(@NotNull List<? extends RsElement> xs) {
        if (xs.isEmpty()) return;
        RsElement last = xs.get(xs.size() - 1);
        var nextSibling = PsiElementUtil.getNextNonCommentSibling(last);
        if (nextSibling != null && PsiElementUtil.getElementType(nextSibling) == RsElementTypes.COMMA) return;
        var comma = new RsPsiFactory(last.getProject(), true, false).createComma();
        last.getParent().addAfter(comma, last);
    }

    private PsiModificationUtils() {}
}
