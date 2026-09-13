/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.completion;

import consulo.language.editor.completion.CompletionUtilCore;
import consulo.language.editor.completion.lookup.InsertionContext;
import consulo.language.editor.completion.lookup.LookupElement;
import consulo.language.editor.completion.lookup.LookupElementBuilder;
import consulo.codeEditor.util.EditorModificationUtil;
import consulo.util.dataholder.Key;
import consulo.language.psi.PsiElement;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.rust.lang.core.psi.ext.RsElement;
import org.rust.lang.core.resolve.KnownItems;
import org.rust.lang.core.resolve.ScopeEntry;

public final class CompletionBridges {
    private CompletionBridges() {
    }

    public static boolean isFnLikeTrait(@Nonnull RsElement element) {
        KnownItems knownItems = KnownItems.getKnownItems(element);
        return element == knownItems.getFn()
            || element == knownItems.getFnMut()
            || element == knownItems.getFnOnce();
    }

    @Nonnull
    public static <T extends PsiElement> T getOriginalOrSelf(@Nonnull T element) {
        return CompletionUtilCore.getOriginalOrSelf(element);
    }

    @Nonnull
    public static <T extends PsiElement> T safeGetOriginalOrSelf(@Nonnull T element) {
        return Utils.safeGetOriginalOrSelf(element);
    }

    public static boolean nextCharIs(@Nonnull InsertionContext context, char c) {
        return LookupElements.nextCharIs(context, c);
    }

    @Nullable
    public static <T extends PsiElement> T getElementOfType(@Nonnull InsertionContext context, @Nonnull Class<T> clazz) {
        return LookupElements.getElementOfType(context, clazz);
    }

    public static void addSuffix(@Nonnull InsertionContext context, @Nonnull String suffix) {
        context.getDocument().insertString(context.getSelectionEndOffset(), suffix);
        EditorModificationUtil.moveCaretRelatively(context.getEditor(), suffix.length());
    }

    @Nonnull
    public static LookupElement withPriority(@Nonnull LookupElementBuilder builder, double priority) {
        return LookupElements.withPriority(builder, priority);
    }

    @Nonnull
    public static Key<Boolean> getFORCE_OUT_OF_SCOPE_COMPLETION() {
        return RsMacroCompletionProvider.FORCE_OUT_OF_SCOPE_COMPLETION;
    }

    @Nonnull
    public static LookupElement createLookupElement(@Nonnull ScopeEntry scopeEntry, @Nonnull RsCompletionContext context) {
        return LookupElements.createLookupElement(scopeEntry, context);
    }
}
