/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.psi.ext;

import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiWhiteSpace;
import org.jetbrains.annotations.NotNull;

/**
 * Bridge class for methods referenced as RsPsiElementUtil.
 */
public final class RsPsiElementExtUtil {

    private RsPsiElementExtUtil() {
    }

    public static int getEndOffsetInParent(@NotNull PsiElement element) {
        return element.getStartOffsetInParent() + element.getTextLength();
    }

    @NotNull
    public static TextRange getRangeWithPrevSpace(@NotNull PsiElement element) {
        TextRange range = element.getTextRange();
        PsiElement prev = element.getPrevSibling();
        if (prev instanceof PsiWhiteSpace) {
            return range.union(prev.getTextRange());
        }
        return range;
    }
}
