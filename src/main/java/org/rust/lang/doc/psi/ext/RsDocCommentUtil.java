/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.doc.psi.ext;

import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.Nullable;
import org.rust.lang.doc.psi.RsDocComment;

/**
 * Utility for doc comment operations.
 */
public final class RsDocCommentUtil {
    private RsDocCommentUtil() {
    }

    /**
     * Returns the containing doc comment for the given element, or null if not inside a doc comment.
     */
    @Nullable
    public static PsiElement getContainingDoc(@Nullable PsiElement element) {
        if (element == null) return null;
        PsiElement current = element;
        while (current != null) {
            if (current instanceof RsDocComment) {
                return current;
            }
            current = current.getParent();
        }
        return null;
    }
}
