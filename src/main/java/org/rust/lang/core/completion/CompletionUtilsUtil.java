/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.completion;

import com.intellij.codeInsight.completion.InsertionContext;
import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.rust.lang.core.psi.ext.RsElement;
import org.rust.lang.core.resolve.KnownItems;

public final class CompletionUtilsUtil {
    private CompletionUtilsUtil() {
    }

    @NotNull
    public static <T extends PsiElement> T safeGetOriginalOrSelf(@NotNull T element) {
        return Utils.safeGetOriginalOrSelf(element);
    }

    @NotNull
    public static <T extends PsiElement> T getOriginalOrSelf(@NotNull T element) {
        return Utils.getOriginalOrSelf(element);
    }

    public static boolean isFnLikeTrait(@NotNull RsElement element) {
        KnownItems knownItems = KnownItems.getKnownItems(element);
        return element.equals(knownItems.getFn())
            || element.equals(knownItems.getFnMut())
            || element.equals(knownItems.getFnOnce());
    }

    public static boolean nextCharIs(@NotNull InsertionContext ctx, char c) {
        return LookupElements.nextCharIs(ctx, c);
    }

    @Nullable
    public static <T extends PsiElement> T getElementOfType(@NotNull InsertionContext ctx, @NotNull Class<T> clazz) {
        return LookupElements.getElementOfType(ctx, clazz);
    }
}
