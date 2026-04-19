/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.macros;

import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.List;

/**
 * Represents a prepared proc macro expansion for highlighting purposes.
 */
public class PreparedProcMacroExpansion {
    private final List<PsiElement> elementsForErrorHighlighting;

    public PreparedProcMacroExpansion(@NotNull List<PsiElement> elements) {
        this.elementsForErrorHighlighting = elements;
    }

    @NotNull
    public List<PsiElement> getElementsForErrorHighlighting() {
        return elementsForErrorHighlighting;
    }
}
