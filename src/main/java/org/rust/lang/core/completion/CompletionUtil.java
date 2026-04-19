/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.completion;

import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.openapi.util.Key;
import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;
import org.rust.lang.core.resolve.ScopeEntry;

/**
 * Delegates to {@link CompletionBridges}, {@link LookupElements}, and {@link RsMacroCompletionProvider}.
 */
public final class CompletionUtil {
    private CompletionUtil() {
    }

    @NotNull
    public static Key<Boolean> getFORCE_OUT_OF_SCOPE_COMPLETION() {
        return RsMacroCompletionProvider.FORCE_OUT_OF_SCOPE_COMPLETION;
    }

    @NotNull
    public static LookupElement createLookupElement(@NotNull ScopeEntry scopeEntry, @NotNull RsCompletionContext context) {
        return CompletionBridges.createLookupElement(scopeEntry, context);
    }

    @NotNull
    public static <T extends PsiElement> T getOriginalOrSelf(@NotNull T element) {
        return CompletionBridges.getOriginalOrSelf(element);
    }

    @NotNull
    public static <T extends PsiElement> T safeGetOriginalOrSelf(@NotNull T element) {
        return CompletionBridges.safeGetOriginalOrSelf(element);
    }
}
