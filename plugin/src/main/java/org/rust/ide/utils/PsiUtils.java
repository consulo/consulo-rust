/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.utils;

import consulo.language.psi.PsiElement;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import consulo.language.psi.PsiFile;
import consulo.language.psi.util.PsiTreeUtil;

public final class PsiUtils {
    private PsiUtils() {
    }

    /**
     * Finds the topmost parent of {@code element} that is still inside {@code container}.
     */
    @Nullable
    public static PsiElement getTopmostParentInside(@Nonnull PsiElement element, @Nonnull PsiElement container) {
        PsiElement current = element;
        PsiElement result = element;
        while (current != null && current != container) {
            result = current;
            current = current.getParent();
        }
        return current == container ? result : null;
    }

    /**
     * Returns the PsiElements in the file that overlap with the given text range [startOffset, endOffset].
     */
    @Nonnull
    public static PsiElement[] getElementRange(@Nonnull consulo.language.psi.PsiFile file, int startOffset, int endOffset) {
        PsiElement startElement = file.findElementAt(startOffset);
        PsiElement endElement = file.findElementAt(endOffset > 0 ? endOffset - 1 : endOffset);
        if (startElement == null || endElement == null) return PsiElement.EMPTY_ARRAY;

        PsiElement commonParent = consulo.language.psi.util.PsiTreeUtil.findCommonParent(startElement, endElement);
        if (commonParent == null) return PsiElement.EMPTY_ARRAY;

        java.util.List<PsiElement> result = new java.util.ArrayList<>();
        for (PsiElement child = commonParent.getFirstChild(); child != null; child = child.getNextSibling()) {
            int childStart = child.getTextRange().getStartOffset();
            int childEnd = child.getTextRange().getEndOffset();
            if (childEnd > startOffset && childStart < endOffset) {
                result.add(child);
            }
        }
        return result.toArray(PsiElement.EMPTY_ARRAY);
    }
}
