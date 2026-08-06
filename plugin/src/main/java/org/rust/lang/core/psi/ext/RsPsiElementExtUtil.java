/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.psi.ext;

import consulo.document.util.TextRange;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiWhiteSpace;
import jakarta.annotation.Nonnull;

/**
 * Bridge class for methods referenced as RsPsiElementUtil.
 */
public final class RsPsiElementExtUtil {

    private RsPsiElementExtUtil() {
    }

    public static int getEndOffsetInParent(@Nonnull PsiElement element) {
        return element.getStartOffsetInParent() + element.getTextLength();
    }

    @Nonnull
    public static TextRange getRangeWithPrevSpace(@Nonnull PsiElement element) {
        TextRange range = element.getTextRange();
        PsiElement prev = element.getPrevSibling();
        if (prev instanceof PsiWhiteSpace) {
            return range.union(prev.getTextRange());
        }
        return range;
    }
}
