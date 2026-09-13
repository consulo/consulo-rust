/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.macros;

import consulo.language.psi.PsiElement;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.rust.lang.core.psi.ext.RsPossibleMacroCall;
import org.rust.lang.core.psi.ext.impl.RsPossibleMacroCallUtil;

import java.util.List;

/**
 * Utility methods for macro expansion operations.
 * Delegates to RsExpandedElementUtil and other utilities.
 */
public final class MacroExpansionUtil {
    private MacroExpansionUtil() {
    }

    @Nullable
    public static PsiElement findElementExpandedFrom(@Nonnull PsiElement element) {
        return RsExpandedElementUtil.findElementExpandedFrom(element);
    }

    @Nonnull
    public static List<PsiElement> findExpansionElements(@Nonnull PsiElement element) {
        return RsExpandedElementUtil.findExpansionElements(element);
    }

    @Nullable
    public static RsPossibleMacroCall findMacroCallExpandedFrom(@Nonnull PsiElement element) {
        return RsExpandedElementUtil.findMacroCallExpandedFrom(element);
    }

    @Nullable
    public static RsPossibleMacroCall findMacroCallExpandedFromNonRecursive(@Nonnull PsiElement element) {
        return RsExpandedElementUtil.findMacroCallExpandedFromNonRecursive(element);
    }

    @Nonnull
    public static MacroExpansionContext getExpansionContext(@Nonnull RsPossibleMacroCall call) {
        return RsPossibleMacroCallUtil.getExpansionContext(call);
    }

    @Nonnull
    public static RangeMap getRanges(@Nonnull MacroExpansion expansion) {
        // The ranges are stored in the expansion file's user data
        RangeMap forced = expansion.getFile().getUserData(RsExpandedElementUtil.RS_FORCED_REDUCED_RANGE_MAP_KEY);
        if (forced != null) return forced;
        return RangeMap.EMPTY;
    }
}
