/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.macros;

import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.rust.lang.core.psi.ext.RsPossibleMacroCall;
import org.rust.lang.core.psi.ext.RsPossibleMacroCallUtil;

import java.util.List;

/**
 * Utility methods for macro expansion operations.
 * Delegates to RsExpandedElementUtil and other utilities.
 */
public final class MacroExpansionUtil {
    private MacroExpansionUtil() {
    }

    @Nullable
    public static PsiElement findElementExpandedFrom(@NotNull PsiElement element) {
        return RsExpandedElementUtil.findElementExpandedFrom(element);
    }

    @NotNull
    public static List<PsiElement> findExpansionElements(@NotNull PsiElement element) {
        return RsExpandedElementUtil.findExpansionElements(element);
    }

    @Nullable
    public static RsPossibleMacroCall findMacroCallExpandedFrom(@NotNull PsiElement element) {
        return RsExpandedElementUtil.findMacroCallExpandedFrom(element);
    }

    @Nullable
    public static RsPossibleMacroCall findMacroCallExpandedFromNonRecursive(@NotNull PsiElement element) {
        return RsExpandedElementUtil.findMacroCallExpandedFromNonRecursive(element);
    }

    @NotNull
    public static MacroExpansionContext getExpansionContext(@NotNull RsPossibleMacroCall call) {
        return RsPossibleMacroCallUtil.getExpansionContext(call);
    }

    @NotNull
    public static RangeMap getRanges(@NotNull MacroExpansion expansion) {
        // The ranges are stored in the expansion file's user data
        RangeMap forced = expansion.getFile().getUserData(RsExpandedElementUtil.RS_FORCED_REDUCED_RANGE_MAP_KEY);
        if (forced != null) return forced;
        return RangeMap.EMPTY;
    }
}
