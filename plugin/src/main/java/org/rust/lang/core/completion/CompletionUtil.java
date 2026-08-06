/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.completion;

import consulo.language.editor.completion.lookup.LookupElement;
import consulo.util.dataholder.Key;
import consulo.language.psi.PsiElement;
import jakarta.annotation.Nonnull;
import org.rust.lang.core.resolve.ScopeEntry;

/**
 * Delegates to {@link CompletionBridges}, {@link LookupElements}, and {@link RsMacroCompletionProvider}.
 */
public final class CompletionUtil {
    private CompletionUtil() {
    }

    @Nonnull
    public static Key<Boolean> getFORCE_OUT_OF_SCOPE_COMPLETION() {
        return RsMacroCompletionProvider.FORCE_OUT_OF_SCOPE_COMPLETION;
    }

    @Nonnull
    public static LookupElement createLookupElement(@Nonnull ScopeEntry scopeEntry, @Nonnull RsCompletionContext context) {
        return CompletionBridges.createLookupElement(scopeEntry, context);
    }

    @Nonnull
    public static <T extends PsiElement> T getOriginalOrSelf(@Nonnull T element) {
        return CompletionBridges.getOriginalOrSelf(element);
    }

    @Nonnull
    public static <T extends PsiElement> T safeGetOriginalOrSelf(@Nonnull T element) {
        return CompletionBridges.safeGetOriginalOrSelf(element);
    }
}
