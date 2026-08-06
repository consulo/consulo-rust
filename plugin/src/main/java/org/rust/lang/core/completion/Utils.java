/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.completion;

import org.rust.lang.core.psi.ext.RsElementUtil;
import consulo.language.editor.completion.CompletionParameters;
import consulo.language.editor.completion.CompletionResultSet;
import consulo.language.editor.completion.CompletionUtilCore;
import consulo.language.editor.completion.CompletionContributor;
import consulo.language.editor.completion.CompletionService;
import consulo.language.editor.completion.CompletionProvider;
import consulo.language.editor.completion.CompletionType;
import consulo.language.editor.completion.CompletionInitializationContext;
import consulo.language.editor.completion.lookup.InsertHandler;
import consulo.language.editor.completion.lookup.InsertionContext;
import consulo.language.editor.completion.lookup.PrioritizedLookupElement;
import consulo.language.editor.completion.lookup.LookupElement;
import consulo.language.editor.completion.lookup.LookupElementBuilder;
import consulo.language.editor.impl.internal.template.LiveTemplateCompletionContributor;
import consulo.language.psi.PsiElement;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.rust.lang.core.psi.ext.RsElement;

import java.util.Iterator;

public final class Utils {
    private Utils() {
    }

    @Nonnull
    public static <T extends PsiElement> T getOriginalOrSelf(@Nonnull T element) {
        return CompletionUtilCore.getOriginalOrSelf(element);
    }

    @Nullable
    @SuppressWarnings("unchecked")
    public static <T extends PsiElement> T safeGetOriginalElement(@Nonnull T element) {
        PsiElement originalElement = element.getOriginalElement();
        if (originalElement == null || originalElement == element) return null;
        T original;
        try {
            original = (T) originalElement;
        } catch (ClassCastException e) {
            return null;
        }
        if (!areAncestorTypesEquals(original, element)) return null;
        return original;
    }

    @Nonnull
    public static <T extends PsiElement> T safeGetOriginalOrSelf(@Nonnull T element) {
        T original = safeGetOriginalElement(element);
        return original != null ? original : element;
    }

    private static boolean areAncestorTypesEquals(@Nonnull PsiElement psi1, @Nonnull PsiElement psi2) {
        Iterator<PsiElement> iter1 = RsElementUtil.getAncestors(psi1).iterator();
        Iterator<PsiElement> iter2 = RsElementUtil.getAncestors(psi2).iterator();
        while (iter1.hasNext() && iter2.hasNext()) {
            PsiElement a = iter1.next();
            PsiElement b = iter2.next();
            if (a.getClass() != b.getClass()) return false;
        }
        return true;
    }

    public static void rerunCompletion(@Nonnull CompletionParameters parameters, @Nonnull CompletionResultSet result) {
        CompletionContributor liveTemplateContributor = null;
        for (CompletionContributor contributor : CompletionContributor.forParameters(parameters)) {
            if (contributor instanceof LiveTemplateCompletionContributor) {
                liveTemplateContributor = contributor;
                break;
            }
        }

        CompletionService.getCompletionService().getVariantsFromContributors(parameters, liveTemplateContributor, completionResult -> {
            result.addElement(completionResult.getLookupElement());
        });
    }
}
