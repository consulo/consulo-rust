/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.openapiext;

import consulo.document.util.TextRange;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiWhiteSpace;
import consulo.language.psi.util.PsiTreeUtil;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.function.Consumer;

/**
 * Bridge class for PsiElement extension functions from the openapiext package.
 */
public final class PsiElementExtUtil {

    private PsiElementExtUtil() {
    }

    @Nullable
    public static <T extends PsiElement> T ancestorStrict(@Nonnull PsiElement element, @Nonnull Class<T> clazz) {
        return PsiTreeUtil.getParentOfType(element, clazz, true);
    }

    @Nullable
    public static <T extends PsiElement> T ancestorOrSelf(@Nonnull PsiElement element, @Nonnull Class<T> clazz) {
        return PsiTreeUtil.getParentOfType(element, clazz, false);
    }

    public static int getEndOffsetInParent(@Nonnull PsiElement element) {
        return element.getStartOffsetInParent() + element.getTextLength();
    }

    @Nonnull
    public static TextRange getRangeWithPrevSpace(@Nonnull PsiElement element) {
        TextRange range = element.getTextRange();
        PsiElement prev = element.getPrevSibling();
        if (prev instanceof PsiWhiteSpace) {
            return range.union(prev.getTextRange());
        }
        return range;
    }

    public static void forEachChild(@Nonnull PsiElement element, @Nonnull Consumer<PsiElement> action) {
        PsiElement child = element.getFirstChild();
        while (child != null) {
            action.accept(child);
            child = child.getNextSibling();
        }
    }
}
