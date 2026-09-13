/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core;

import consulo.language.pattern.PlatformPatterns;
import consulo.language.pattern.ElementPattern;
import consulo.language.pattern.PsiElementPattern;
import consulo.language.pattern.ObjectPattern;
import consulo.language.pattern.PatternCondition;
import consulo.language.psi.PsiComment;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiErrorElement;
import consulo.language.psi.PsiWhiteSpace;
import consulo.language.util.ProcessingContext;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import consulo.language.psi.util.PsiTreeUtil;

public final class RsPsiPatternUtil {
    private RsPsiPatternUtil() {}

    @Nullable
    public static PsiElement getPrevVisibleOrNewLine(@Nonnull PsiElement element) {
        PsiElement current = element.getPrevSibling();
        // Walk left leaves
        if (current == null) {
            PsiElement parent = element.getParent();
            if (parent != null) {
                current = parent.getPrevSibling();
            }
        }
        while (current != null) {
            if (current instanceof PsiComment || current instanceof PsiErrorElement) {
                current = current.getPrevSibling();
                continue;
            }
            if (current instanceof PsiWhiteSpace) {
                if (current.getText().contains("\n")) {
                    return current;
                }
                current = current.getPrevSibling();
                continue;
            }
            return current;
        }
        return null;
    }

    /**
     * Helper for creating typed PsiElementPattern.Capture with a class.
     */
    public static <I extends PsiElement> PsiElementPattern.Capture<I> psiElement(Class<I> cls) {
        return PlatformPatterns.psiElement(cls);
    }

    /**
     * Extension-function equivalent: ObjectPattern.with(name, cond).
     */
    public static <T, Self extends ObjectPattern<T, Self>> Self withCondition(
        ObjectPattern<T, Self> pattern,
        String name,
        java.util.function.Predicate<T> cond
    ) {
        return pattern.with(new PatternCondition<T>(name) {
            @Override
            public boolean accepts(@Nonnull T t, ProcessingContext context) {
                return cond.test(t);
            }
        });
    }

    /**
     * Extension-function equivalent: withPrevSiblingSkipping.
     * Similar with TreeElementPattern.afterSiblingSkipping
     * but it uses PsiElement.getPrevSibling to get previous sibling elements.
     */
    public static <T extends PsiElement, Self extends PsiElementPattern<T, Self>> Self withPrevSiblingSkipping(
        PsiElementPattern<T, Self> pattern,
        ElementPattern<? extends PsiElement> skip,
        ElementPattern<? extends T> target
    ) {
        return pattern.with(new PatternCondition<T>("withPrevSiblingSkipping") {
            @Override
            public boolean accepts(@Nonnull T e, ProcessingContext context) {
                PsiElement sibling = e.getPrevSibling();
                while (sibling != null && skip.accepts(sibling)) {
                    sibling = sibling.getPrevSibling();
                }
                if (sibling == null) return false;
                return target.accepts(sibling);
            }
        });
    }

    /**
     * Extension-function equivalent: withPrevLeafSkipping.
     */
    public static <T extends PsiElement, Self extends PsiElementPattern<T, Self>> Self withPrevLeafSkipping(
        PsiElementPattern<T, Self> pattern,
        ElementPattern<? extends PsiElement> skip,
        ElementPattern<? extends T> target
    ) {
        return pattern.with(new PatternCondition<T>("withPrevSiblingSkipping") {
            @Override
            public boolean accepts(@Nonnull T e, ProcessingContext context) {
                PsiElement leaf = consulo.language.psi.util.PsiTreeUtil.prevLeaf(e);
                while (leaf != null && skip.accepts(leaf)) {
                    leaf = consulo.language.psi.util.PsiTreeUtil.prevLeaf(leaf);
                }
                if (leaf == null) return false;
                return target.accepts(leaf);
            }
        });
    }
}
