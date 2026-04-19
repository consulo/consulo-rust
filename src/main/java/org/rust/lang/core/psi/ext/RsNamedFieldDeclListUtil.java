/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.psi.ext;

import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;
import org.rust.lang.core.psi.RsElementTypes;
import org.rust.lang.core.psi.RsPsiFactory;

import java.util.List;

/**
 * Utility methods for lists of named field declarations.
 */
public final class RsNamedFieldDeclListUtil {
    private RsNamedFieldDeclListUtil() {
    }

    public static void ensureTrailingComma(@NotNull List<? extends RsElement> elements) {
        if (elements.isEmpty()) return;
        RsElement last = elements.get(elements.size() - 1);
        PsiElement nextSibling = last.getNextSibling();
        while (nextSibling != null && (nextSibling instanceof com.intellij.psi.PsiWhiteSpace || nextSibling instanceof com.intellij.psi.PsiComment)) {
            nextSibling = nextSibling.getNextSibling();
        }
        if (nextSibling != null && nextSibling.getNode().getElementType() == RsElementTypes.COMMA) return;
        PsiElement comma = new RsPsiFactory(last.getProject()).createComma();
        last.getParent().addAfter(comma, last);
    }
}
