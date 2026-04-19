/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.completion;

import org.rust.lang.core.psi.ext.RsElementUtil;
import com.intellij.codeInsight.completion.*;
import com.intellij.codeInsight.template.impl.LiveTemplateCompletionContributor;
import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.rust.lang.core.psi.ext.RsElement;

import java.util.Iterator;

public final class Utils {
    private Utils() {
    }

    @NotNull
    public static <T extends PsiElement> T getOriginalOrSelf(@NotNull T element) {
        return CompletionUtil.getOriginalOrSelf(element);
    }

    @Nullable
    @SuppressWarnings("unchecked")
    public static <T extends PsiElement> T safeGetOriginalElement(@NotNull T element) {
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

    @NotNull
    public static <T extends PsiElement> T safeGetOriginalOrSelf(@NotNull T element) {
        T original = safeGetOriginalElement(element);
        return original != null ? original : element;
    }

    private static boolean areAncestorTypesEquals(@NotNull PsiElement psi1, @NotNull PsiElement psi2) {
        Iterator<PsiElement> iter1 = RsElementUtil.getAncestors(psi1).iterator();
        Iterator<PsiElement> iter2 = RsElementUtil.getAncestors(psi2).iterator();
        while (iter1.hasNext() && iter2.hasNext()) {
            PsiElement a = iter1.next();
            PsiElement b = iter2.next();
            if (a.getClass() != b.getClass()) return false;
        }
        return true;
    }

    public static void rerunCompletion(@NotNull CompletionParameters parameters, @NotNull CompletionResultSet result) {
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
