/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.openapiext;

/**
 * Bridge class delegating to {@link PsiUtil}.
 */
public final class ProcessElementsWithMacrosUtil {
    private ProcessElementsWithMacrosUtil() {
    }

    public static boolean processElementsWithMacros(
        @org.jetbrains.annotations.NotNull com.intellij.psi.PsiElement element,
        @org.jetbrains.annotations.NotNull PsiTreeProcessor processor
    ) {
        return PsiUtil.processElementsWithMacros(element, processor);
    }
}
