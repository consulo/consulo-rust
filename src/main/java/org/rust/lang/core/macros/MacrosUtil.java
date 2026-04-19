/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.macros;

import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
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

    @NotNull
    public static List<PsiElement> findExpansionElements(@NotNull PsiElement element, @Nullable Object attrCache) {
        return RsExpandedElementUtil.findExpansionElements(element);
    }

    @Nullable
    public static MacroCallPreparedForHighlighting prepareForExpansionHighlighting(@NotNull RsPossibleMacroCall call) {
        return MacroHighlightingUtil.prepareForExpansionHighlighting(call);
    }

    @Nullable
    public static MacroCallPreparedForHighlighting prepareForExpansionHighlighting(
        @NotNull RsPossibleMacroCall call,
        @Nullable MacroCallPreparedForHighlighting ancestorMacro
    ) {
        return MacroHighlightingUtil.prepareForExpansionHighlighting(call, ancestorMacro);
    }

    @NotNull
    public static List<TextRange> mapRangeFromExpansionToCallBody(
        @NotNull MacroExpansion expansion,
        @NotNull RsPossibleMacroCall call,
        @NotNull TextRange range
    ) {
        return RsExpandedElementUtil.mapRangeFromExpansionToCallBody(call, range);
    }

    public static void setContext(@NotNull RsModItem element, @NotNull RsMod context) {
        RsExpandedElementUtil.setContext(element, context);
    }
}
