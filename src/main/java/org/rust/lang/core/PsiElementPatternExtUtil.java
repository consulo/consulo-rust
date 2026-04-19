/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core;

import com.intellij.patterns.*;
import com.intellij.psi.PsiElement;
import com.intellij.psi.tree.IElementType;
import org.jetbrains.annotations.NotNull;

public final class PsiElementPatternExtUtil {
    private PsiElementPatternExtUtil() {}

    /**
     * Creates a PsiElementPattern.Capture for the given PsiElement class.
     * Equivalent to PlatformPatterns.psiElement(cls).
     */
    @NotNull
    public static <I extends PsiElement> PsiElementPattern.Capture<I> psiElement(@NotNull Class<I> cls) {
        return PlatformPatterns.psiElement(cls);
    }

    /**
     * Combines two element patterns with OR logic.
     * Equivalent to StandardPatterns.or(pattern1, pattern2).
     */
    @NotNull
    @SuppressWarnings("unchecked")
    public static <T> ElementPattern<T> or(@NotNull ElementPattern<? extends T> pattern1,
                                           @NotNull ElementPattern<? extends T> pattern2) {
        return (ElementPattern<T>) StandardPatterns.or(pattern1, pattern2);
    }

    /**
     * Adds a withSuperParent constraint to the pattern.
     * Equivalent to pattern.withSuperParent(level, superParentClass).
     */
    @NotNull
    @SuppressWarnings("unchecked")
    public static <T extends PsiElement> PsiElementPattern.Capture<T> withSuperParent(
        @NotNull ElementPattern<T> pattern,
        int level,
        @NotNull Class<? extends PsiElement> superParentClass
    ) {
        if (pattern instanceof PsiElementPattern.Capture) {
            return ((PsiElementPattern.Capture<T>) pattern).withSuperParent(level, superParentClass);
        }
        // Wrap into a Capture first
        return PlatformPatterns.psiElement((Class<T>) PsiElement.class)
            .and(pattern)
            .withSuperParent(level, superParentClass);
    }

    /**
     * Adds a withElementType constraint to the pattern.
     * Equivalent to pattern.withElementType(elementType).
     */
    @NotNull
    @SuppressWarnings("unchecked")
    public static <T extends PsiElement> PsiElementPattern.Capture<T> withElementType(
        @NotNull PsiElementPattern.Capture<T> pattern,
        @NotNull IElementType elementType
    ) {
        return pattern.withElementType(elementType);
    }
}
