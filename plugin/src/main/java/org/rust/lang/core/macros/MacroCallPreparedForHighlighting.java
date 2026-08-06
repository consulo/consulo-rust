/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.macros;

import consulo.language.editor.util.CollectHighlightsUtil;
import consulo.language.psi.PsiElement;
import jakarta.annotation.Nonnull;
import org.rust.lang.core.psi.ext.RsPossibleMacroCall;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

public final class MacroCallPreparedForHighlighting {
    private final RsPossibleMacroCall myMacroCall;
    private final MacroExpansion myExpansion;
    private final boolean myIsDeeplyAttrMacro;

    public MacroCallPreparedForHighlighting(
        @Nonnull RsPossibleMacroCall macroCall,
        @Nonnull MacroExpansion expansion,
        boolean isDeeplyAttrMacro
    ) {
        myMacroCall = macroCall;
        myExpansion = expansion;
        myIsDeeplyAttrMacro = isDeeplyAttrMacro;
    }

    @Nonnull
    public RsPossibleMacroCall getMacroCall() {
        return myMacroCall;
    }

    @Nonnull
    public MacroExpansion getExpansion() {
        return myExpansion;
    }

    public boolean isDeeplyAttrMacro() {
        return myIsDeeplyAttrMacro;
    }

    @Nonnull
    public List<PsiElement> getElementsForHighlighting() {
        if (MacroExpansionUtil.getRanges(myExpansion).isEmpty()) return Collections.emptyList();
        return getElementsForErrorHighlighting();
    }

    @Nonnull
    public List<PsiElement> getElementsForErrorHighlighting() {
        return CollectHighlightsUtil.getElementsInRange(myExpansion.getFile(), 0, myExpansion.getFile().getTextLength());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        MacroCallPreparedForHighlighting that = (MacroCallPreparedForHighlighting) o;
        return myIsDeeplyAttrMacro == that.myIsDeeplyAttrMacro
            && myMacroCall.equals(that.myMacroCall)
            && myExpansion.equals(that.myExpansion);
    }

    @Override
    public int hashCode() {
        return Objects.hash(myMacroCall, myExpansion, myIsDeeplyAttrMacro);
    }
}
