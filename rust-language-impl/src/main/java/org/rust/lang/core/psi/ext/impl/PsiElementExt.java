/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.psi.ext.impl;

import consulo.document.util.TextRange;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiFile;
import consulo.language.psi.PsiWhiteSpace;
import consulo.language.ast.IElementType;
import consulo.language.psi.util.PsiTreeUtil;
import consulo.language.psi.PsiUtilCore;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.Iterator;
import org.rust.lang.doc.psi.RsDocComment;
import org.rust.lang.doc.psi.ext.RsDocPsiElementUtil;
import org.rust.lang.core.psi.ext.*;

public final class PsiElementExt {

    private PsiElementExt() {
    }

    @Nullable
    public static <T extends PsiElement> T ancestorStrict(@Nonnull PsiElement element, @Nonnull Class<T> clazz) {
        return PsiTreeUtil.getParentOfType(element, clazz, true);
    }

    @Nullable
    public static <T extends PsiElement> T ancestorOrSelf(@Nonnull PsiElement element, @Nonnull Class<T> clazz) {
        return PsiTreeUtil.getParentOfType(element, clazz, false);
    }

    @Nullable
    public static <T extends PsiElement> T contextStrict(@Nonnull PsiElement element, @Nonnull Class<T> clazz) {
        return PsiTreeUtil.getContextOfType(element, clazz, true);
    }

    @Nullable
    public static <T extends PsiElement> T descendantOfTypeOrSelf(@Nonnull PsiElement element, @Nonnull Class<T> clazz) {
        return PsiTreeUtil.findChildOfType(element, clazz, false);
    }

    @Nonnull
    public static IElementType getElementType(@Nonnull PsiElement element) {
        return PsiUtilCore.getElementType(element);
    }

    public static int getStartOffset(@Nonnull PsiElement element) {
        return element.getTextRange().getStartOffset();
    }

    public static int getEndOffset(@Nonnull PsiElement element) {
        return element.getTextRange().getEndOffset();
    }

    @Nullable
    public static PsiElement getNextNonCommentSibling(@Nullable PsiElement element) {
        return PsiTreeUtil.skipWhitespacesAndCommentsForward(element);
    }

    @Nullable
    public static PsiElement getNextNonWhitespaceSibling(@Nullable PsiElement element) {
        return PsiTreeUtil.skipWhitespacesForward(element);
    }

    /** Finds first sibling that is neither comment, nor whitespace before given element */
    @Nullable
    public static PsiElement getPrevNonCommentSibling(@Nullable PsiElement element) {
        return PsiTreeUtil.skipWhitespacesAndCommentsBackward(element);
    }

    @Nullable
    public static PsiElement getPrevNonWhitespaceSibling(@Nullable PsiElement element) {
        return PsiTreeUtil.skipWhitespacesBackward(element);
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

    @Nonnull
    public static Iterable<PsiElement> getRightSiblings(@Nonnull PsiElement element) {
        return new Iterable<PsiElement>() {
            public Iterator<PsiElement> iterator() {
                return new Iterator<PsiElement>() {
                    PsiElement current = element.getNextSibling();
                    public boolean hasNext() { return current != null; }
                    public PsiElement next() { PsiElement r = current; current = current.getNextSibling(); return r; }
                };
            }
        };
    }

    @Nonnull
    public static Iterable<PsiElement> getLeftSiblings(@Nonnull PsiElement element) {
        return new Iterable<PsiElement>() {
            public Iterator<PsiElement> iterator() {
                return new Iterator<PsiElement>() {
                    PsiElement current = element.getPrevSibling();
                    public boolean hasNext() { return current != null; }
                    public PsiElement next() { PsiElement r = current; current = current.getPrevSibling(); return r; }
                };
            }
        };
    }

    public static boolean getExistsAfterExpansion(@Nonnull PsiElement element) {
        return CfgUtils.existsAfterExpansion(element);
    }

    public static boolean isEnabledByCfg(@Nonnull PsiElement element) {
        return CfgUtils.isEnabledByCfg(element);
    }

    public static boolean isContextOf(@Nonnull PsiElement ancestor, @Nonnull PsiElement child) {
        PsiElement current = child;
        while (current != null) {
            if (current.equals(ancestor)) return true;
            if (current instanceof PsiFile) break;
            current = current.getContext();
        }
        return false;
    }

    public static boolean isIntentionPreviewElement(@Nonnull PsiElement element) {
        return com.intellij.codeInsight.intention.preview.IntentionPreviewUtils.isPreviewElement(element);
    }

    @Nullable
    public static org.rust.lang.doc.psi.RsDocComment containingDoc(@Nonnull PsiElement element) {
        return org.rust.lang.doc.psi.ext.RsDocPsiElementUtil.containingDoc(element);
    }

    public static boolean isInDocComment(@Nonnull PsiElement element) {
        return org.rust.lang.doc.psi.ext.RsDocPsiElementUtil.isInDocComment(element);
    }
}
