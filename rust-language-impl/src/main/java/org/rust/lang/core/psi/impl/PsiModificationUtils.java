/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.psi.impl;

import jakarta.annotation.Nonnull;
import org.rust.lang.core.psi.ext.RsElement;
import org.rust.lang.core.psi.ext.impl.PsiElementUtil;

import java.util.List;
import org.rust.lang.core.psi.*;

public final class PsiModificationUtils {

    public static void ensureTrailingComma(@Nonnull List<? extends RsElement> xs) {
        if (xs.isEmpty()) return;
        RsElement last = xs.get(xs.size() - 1);
        var nextSibling = PsiElementUtil.getNextNonCommentSibling(last);
        if (nextSibling != null && PsiElementUtil.getElementType(nextSibling) == RsElementTypes.COMMA) return;
        var comma = new RsPsiFactory(last.getProject(), true, false).createComma();
        last.getParent().addAfter(comma, last);
    }

    private PsiModificationUtils() {}
}
