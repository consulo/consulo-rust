/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.psi.ext;

import org.rust.lang.core.psi.ext.PsiElementUtil;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiWhiteSpace;
import com.intellij.psi.tree.IElementType;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.psi.util.PsiUtilCore;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Iterator;

/**
 * Some converted Java code references PsiElementExt instead of PsiElementUtil.
 */
public final class PsiElementExt {

    private PsiElementExt() {
    }

    @Nullable
    public static <T extends PsiElement> T ancestorStrict(@NotNull PsiElement element, @NotNull Class<T> clazz) {
        return PsiTreeUtil.getParentOfType(element, clazz, true);
    }

    @Nullable
    public static <T extends PsiElement> T ancestorOrSelf(@NotNull PsiElement element, @NotNull Class<T> clazz) {
        return PsiTreeUtil.getParentOfType(element, clazz, false);
    }

    @Nullable
    public static <T extends PsiElement> T contextStrict(@NotNull PsiElement element, @NotNull Class<T> clazz) {
        return PsiTreeUtil.getContextOfType(element, clazz, true);
    }

    @Nullable
    public static <T extends PsiElement> T descendantOfTypeOrSelf(@NotNull PsiElement element, @NotNull Class<T> clazz) {
        return PsiTreeUtil.findChildOfType(element, clazz, false);
    }

    @NotNull
    public static IElementType getElementType(@NotNull PsiElement element) {
        return PsiUtilCore.getElementType(element);
    }

    public static int getStartOffset(@NotNull PsiElement element) {
        return element.getTextRange().getStartOffset();
    }

    public static int getEndOffset(@NotNull PsiElement element) {
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

    @NotNull
    public static TextRange getRangeWithPrevSpace(@NotNull PsiElement element) {
        TextRange range = element.getTextRange();
        PsiElement prev = element.getPrevSibling();
        if (prev instanceof PsiWhiteSpace) {
            return range.union(prev.getTextRange());
        }
        return range;
    }

    @NotNull
    public static Iterable<PsiElement> getRightSiblings(@NotNull PsiElement element) {
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

    @NotNull
    public static Iterable<PsiElement> getLeftSiblings(@NotNull PsiElement element) {
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

    public static boolean getExistsAfterExpansion(@NotNull PsiElement element) {
        return CfgUtils.existsAfterExpansion(element);
    }

    public static boolean isEnabledByCfg(@NotNull PsiElement element) {
        return CfgUtils.isEnabledByCfg(element);
    }

    public static boolean isContextOf(@NotNull PsiElement ancestor, @NotNull PsiElement child) {
        PsiElement current = child;
        while (current != null) {
            if (current.equals(ancestor)) return true;
            if (current instanceof PsiFile) break;
            current = current.getContext();
        }
        return false;
    }

    public static boolean isIntentionPreviewElement(@NotNull PsiElement element) {
        return com.intellij.codeInsight.intention.preview.IntentionPreviewUtils.isPreviewElement(element);
    }

    @Nullable
    public static org.rust.lang.doc.psi.RsDocComment containingDoc(@NotNull PsiElement element) {
        return org.rust.lang.doc.psi.ext.RsDocPsiElementUtil.containingDoc(element);
    }

    public static boolean isInDocComment(@NotNull PsiElement element) {
        return org.rust.lang.doc.psi.ext.RsDocPsiElementUtil.isInDocComment(element);
    }
}
