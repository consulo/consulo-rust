/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.openapiext;

import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiWhiteSpace;
import com.intellij.psi.util.PsiTreeUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.function.Consumer;

/**
 * Bridge class for PsiElement extension functions from the openapiext package.
 */
public final class PsiElementExtUtil {

    private PsiElementExtUtil() {
    }

    @Nullable
    public static <T extends PsiElement> T ancestorStrict(@NotNull PsiElement element, @NotNull Class<T> clazz) {
        return PsiTreeUtil.getParentOfType(element, clazz, true);
    }

    @Nullable
    public static <T extends PsiElement> T ancestorOrSelf(@NotNull PsiElement element, @NotNull Class<T> clazz) {
        return PsiTreeUtil.getParentOfType(element, clazz, false);
    }

    public static int getEndOffsetInParent(@NotNull PsiElement element) {
        return element.getStartOffsetInParent() + element.getTextLength();
    }

    @NotNull
    public static TextRange getRangeWithPrevSpace(@NotNull PsiElement element) {
        TextRange range = element.getTextRange();
        PsiElement prev = element.getPrevSibling();
        if (prev instanceof PsiWhiteSpace) {
            return range.union(prev.getTextRange());
        }
        return range;
    }

    public static void forEachChild(@NotNull PsiElement element, @NotNull Consumer<PsiElement> action) {
        PsiElement child = element.getFirstChild();
        while (child != null) {
            action.accept(child);
            child = child.getNextSibling();
        }
    }
}
