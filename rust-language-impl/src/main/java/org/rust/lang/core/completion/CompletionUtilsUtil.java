/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.completion;

import consulo.language.editor.completion.lookup.InsertionContext;
import consulo.language.psi.PsiElement;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.rust.lang.core.psi.ext.RsElement;
import org.rust.lang.core.resolve.KnownItems;

public final class CompletionUtilsUtil {
    private CompletionUtilsUtil() {
    }

    @Nonnull
    public static <T extends PsiElement> T safeGetOriginalOrSelf(@Nonnull T element) {
        return Utils.safeGetOriginalOrSelf(element);
    }

    @Nonnull
    public static <T extends PsiElement> T getOriginalOrSelf(@Nonnull T element) {
        return Utils.getOriginalOrSelf(element);
    }

    public static boolean isFnLikeTrait(@Nonnull RsElement element) {
        KnownItems knownItems = KnownItems.getKnownItems(element);
        return element.equals(knownItems.getFn())
            || element.equals(knownItems.getFnMut())
            || element.equals(knownItems.getFnOnce());
    }

    public static boolean nextCharIs(@Nonnull InsertionContext ctx, char c) {
        return LookupElements.nextCharIs(ctx, c);
    }

    @Nullable
    public static <T extends PsiElement> T getElementOfType(@Nonnull InsertionContext ctx, @Nonnull Class<T> clazz) {
        return LookupElements.getElementOfType(ctx, clazz);
    }
}
