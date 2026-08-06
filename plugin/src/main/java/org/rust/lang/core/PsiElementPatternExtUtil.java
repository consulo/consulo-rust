/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core;

import consulo.language.pattern.PlatformPatterns;
import consulo.language.pattern.ElementPattern;
import consulo.language.pattern.PsiElementPattern;
import consulo.language.pattern.StandardPatterns;
import consulo.language.psi.PsiElement;
import consulo.language.ast.IElementType;
import jakarta.annotation.Nonnull;

public final class PsiElementPatternExtUtil {
    private PsiElementPatternExtUtil() {}

    /**
     * Creates a PsiElementPattern.Capture for the given PsiElement class.
     * Equivalent to PlatformPatterns.psiElement(cls).
     */
    @Nonnull
    public static <I extends PsiElement> PsiElementPattern.Capture<I> psiElement(@Nonnull Class<I> cls) {
        return PlatformPatterns.psiElement(cls);
    }

    /**
     * Combines two element patterns with OR logic.
     * Equivalent to StandardPatterns.or(pattern1, pattern2).
     */
    @Nonnull
    @SuppressWarnings("unchecked")
    public static <T> ElementPattern<T> or(@Nonnull ElementPattern<? extends T> pattern1,
                                           @Nonnull ElementPattern<? extends T> pattern2) {
        return (ElementPattern<T>) StandardPatterns.or(pattern1, pattern2);
    }

    /**
     * Adds a withSuperParent constraint to the pattern.
     * Equivalent to pattern.withSuperParent(level, superParentClass).
     */
    @Nonnull
    @SuppressWarnings("unchecked")
    public static <T extends PsiElement> PsiElementPattern.Capture<T> withSuperParent(
        @Nonnull ElementPattern<T> pattern,
        int level,
        @Nonnull Class<? extends PsiElement> superParentClass
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
    @Nonnull
    @SuppressWarnings("unchecked")
    public static <T extends PsiElement> PsiElementPattern.Capture<T> withElementType(
        @Nonnull PsiElementPattern.Capture<T> pattern,
        @Nonnull IElementType elementType
    ) {
        return pattern.withElementType(elementType);
    }
}
