/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.completion;

import com.intellij.codeInsight.completion.CompletionUtil;
import com.intellij.codeInsight.completion.InsertionContext;
import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.codeInsight.lookup.LookupElementBuilder;
import com.intellij.openapi.editor.EditorModificationUtil;
import com.intellij.openapi.util.Key;
import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.rust.lang.core.psi.ext.RsElement;
import org.rust.lang.core.resolve.KnownItems;
import org.rust.lang.core.resolve.ScopeEntry;

public final class CompletionBridges {
    private CompletionBridges() {
    }

    public static boolean isFnLikeTrait(@NotNull RsElement element) {
        KnownItems knownItems = KnownItems.getKnownItems(element);
        return element == knownItems.getFn()
            || element == knownItems.getFnMut()
            || element == knownItems.getFnOnce();
    }

    @NotNull
    public static <T extends PsiElement> T getOriginalOrSelf(@NotNull T element) {
        return CompletionUtil.getOriginalOrSelf(element);
    }

    @NotNull
    public static <T extends PsiElement> T safeGetOriginalOrSelf(@NotNull T element) {
        return Utils.safeGetOriginalOrSelf(element);
    }

    public static boolean nextCharIs(@NotNull InsertionContext context, char c) {
        return LookupElements.nextCharIs(context, c);
    }

    @Nullable
    public static <T extends PsiElement> T getElementOfType(@NotNull InsertionContext context, @NotNull Class<T> clazz) {
        return LookupElements.getElementOfType(context, clazz);
    }

    public static void addSuffix(@NotNull InsertionContext context, @NotNull String suffix) {
        context.getDocument().insertString(context.getSelectionEndOffset(), suffix);
        EditorModificationUtil.moveCaretRelatively(context.getEditor(), suffix.length());
    }

    @NotNull
    public static LookupElement withPriority(@NotNull LookupElementBuilder builder, double priority) {
        return LookupElements.withPriority(builder, priority);
    }

    @NotNull
    public static Key<Boolean> getFORCE_OUT_OF_SCOPE_COMPLETION() {
        return RsMacroCompletionProvider.FORCE_OUT_OF_SCOPE_COMPLETION;
    }

    @NotNull
    public static LookupElement createLookupElement(@NotNull ScopeEntry scopeEntry, @NotNull RsCompletionContext context) {
        return LookupElements.createLookupElement(scopeEntry, context);
    }
}
