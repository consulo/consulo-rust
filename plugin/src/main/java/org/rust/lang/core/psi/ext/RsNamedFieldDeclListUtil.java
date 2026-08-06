/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.psi.ext;

import consulo.language.psi.PsiElement;
import jakarta.annotation.Nonnull;
import org.rust.lang.core.psi.RsElementTypes;
import org.rust.lang.core.psi.RsPsiFactory;

import java.util.List;

/**
 * Utility methods for lists of named field declarations.
 */
public final class RsNamedFieldDeclListUtil {
    private RsNamedFieldDeclListUtil() {
    }

    public static void ensureTrailingComma(@Nonnull List<? extends RsElement> elements) {
        if (elements.isEmpty()) return;
        RsElement last = elements.get(elements.size() - 1);
        PsiElement nextSibling = last.getNextSibling();
        while (nextSibling != null && (nextSibling instanceof consulo.language.psi.PsiWhiteSpace || nextSibling instanceof consulo.language.psi.PsiComment)) {
            nextSibling = nextSibling.getNextSibling();
        }
        if (nextSibling != null && nextSibling.getNode().getElementType() == RsElementTypes.COMMA) return;
        PsiElement comma = new RsPsiFactory(last.getProject()).createComma();
        last.getParent().addAfter(comma, last);
    }
}
