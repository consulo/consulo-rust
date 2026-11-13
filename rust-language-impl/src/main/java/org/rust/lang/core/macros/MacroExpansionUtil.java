/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.macros;

import consulo.language.psi.PsiElement;
import consulo.util.dataholder.Key;
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
    /**
     * The mapping between the macro call body and the text of an in-memory expansion file.
     */
    public static final Key<RangeMap> MACRO_RANGE_MAP_KEY = Key.create("org.rust.lang.core.macros.MACRO_RANGE_MAP");

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
        RangeMap forced = expansion.getFile().getUserData(RsExpandedElementUtil.RS_FORCED_REDUCED_RANGE_MAP_KEY);
        if (forced != null) return forced;
        RangeMap ranges = expansion.getFile().getUserData(MACRO_RANGE_MAP_KEY);
        return ranges != null ? ranges : RangeMap.EMPTY;
    }
}
