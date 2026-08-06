/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.macros;

import consulo.language.psi.PsiElement;
import jakarta.annotation.Nonnull;

import java.util.Collections;
import java.util.List;

/**
 * Represents a prepared proc macro expansion for highlighting purposes.
 */
public class PreparedProcMacroExpansion {
    private final List<PsiElement> elementsForErrorHighlighting;

    public PreparedProcMacroExpansion(@Nonnull List<PsiElement> elements) {
        this.elementsForErrorHighlighting = elements;
    }

    @Nonnull
    public List<PsiElement> getElementsForErrorHighlighting() {
        return elementsForErrorHighlighting;
    }
}
