/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.macros;

import consulo.document.util.TextRange;
import consulo.language.psi.PsiElement;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.rust.lang.core.psi.ext.RsPossibleMacroCall;

import java.util.List;

/**
 * Delegates to RsExpandedElementUtil for macro expansion utility methods.
 */
public final class MacroExpansionExtUtil {
    private MacroExpansionExtUtil() {
    }

    @Nonnull
    public static List<PsiElement> findExpansionElements(@Nonnull PsiElement element) {
        return RsExpandedElementUtil.findExpansionElements(element);
    }

    @Nonnull
    public static List<PsiElement> findExpansionElements(@Nonnull PsiElement element, @Nullable Object attrCache) {
        return RsExpandedElementUtil.findExpansionElements(element);
    }

    @Nonnull
    public static PsiElement findExpansionElementOrSelf(@Nonnull PsiElement element) {
        return RsExpandedElementUtil.findExpansionElementOrSelf(element);
    }

    @Nullable
    public static RsPossibleMacroCall findMacroCallExpandedFromNonRecursive(@Nonnull PsiElement element) {
        return RsExpandedElementUtil.findMacroCallExpandedFromNonRecursive(element);
    }

    @Nullable
    public static TextRange mapRangeFromExpansionToCallBodyStrict(@Nonnull PsiElement call, @Nonnull TextRange range) {
        return RsExpandedElementUtil.mapRangeFromExpansionToCallBodyStrict(call, range);
    }
}
