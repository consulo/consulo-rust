/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.macros;

import consulo.document.util.TextRange;
import consulo.language.psi.PsiElement;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.rust.lang.core.psi.RsModItem;
import org.rust.lang.core.macros.RsExpandedElementUtil;
import org.rust.lang.core.psi.ext.RsMod;
import org.rust.lang.core.psi.ext.RsPossibleMacroCall;

import java.util.List;

/**
 * Utility methods for macro operations - delegates to RsExpandedElementUtil and MacroHighlightingUtil.
 */
public final class MacrosUtil {
    private MacrosUtil() {
    }

    @Nonnull
    public static List<PsiElement> findExpansionElements(@Nonnull PsiElement element, @Nullable Object attrCache) {
        return RsExpandedElementUtil.findExpansionElements(element);
    }

    @Nullable
    public static MacroCallPreparedForHighlighting prepareForExpansionHighlighting(@Nonnull RsPossibleMacroCall call) {
        return MacroHighlightingUtil.prepareForExpansionHighlighting(call);
    }

    @Nullable
    public static MacroCallPreparedForHighlighting prepareForExpansionHighlighting(
        @Nonnull RsPossibleMacroCall call,
        @Nullable MacroCallPreparedForHighlighting ancestorMacro
    ) {
        return MacroHighlightingUtil.prepareForExpansionHighlighting(call, ancestorMacro);
    }

    @Nonnull
    public static List<TextRange> mapRangeFromExpansionToCallBody(
        @Nonnull MacroExpansion expansion,
        @Nonnull RsPossibleMacroCall call,
        @Nonnull TextRange range
    ) {
        return RsExpandedElementUtil.mapRangeFromExpansionToCallBody(call, range);
    }

    public static void setContext(@Nonnull RsModItem element, @Nonnull RsMod context) {
        RsExpandedElementUtil.setContext(element, context);
    }
}
