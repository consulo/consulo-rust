/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.macros;

import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.rust.lang.core.psi.ext.RsPossibleMacroCall;

import java.util.List;

/**
 * Delegates to RsExpandedElementUtil for macro expansion utility methods.
 */
public final class MacroExpansionExtUtil {
    private MacroExpansionExtUtil() {
    }

    @NotNull
    public static List<PsiElement> findExpansionElements(@NotNull PsiElement element) {
        return RsExpandedElementUtil.findExpansionElements(element);
    }

    @NotNull
    public static List<PsiElement> findExpansionElements(@NotNull PsiElement element, @Nullable Object attrCache) {
        return RsExpandedElementUtil.findExpansionElements(element);
    }

    @NotNull
    public static PsiElement findExpansionElementOrSelf(@NotNull PsiElement element) {
        return RsExpandedElementUtil.findExpansionElementOrSelf(element);
    }

    @Nullable
    public static RsPossibleMacroCall findMacroCallExpandedFromNonRecursive(@NotNull PsiElement element) {
        return RsExpandedElementUtil.findMacroCallExpandedFromNonRecursive(element);
    }

    @Nullable
    public static TextRange mapRangeFromExpansionToCallBodyStrict(@NotNull PsiElement call, @NotNull TextRange range) {
        return RsExpandedElementUtil.mapRangeFromExpansionToCallBodyStrict(call, range);
    }
}
