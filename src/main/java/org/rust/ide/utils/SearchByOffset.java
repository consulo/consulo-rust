/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.utils;

import com.intellij.psi.PsiComment;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiWhiteSpace;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.psi.util.PsiUtilCore;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.rust.lang.core.macros.MacroExpansionContext;
import org.rust.lang.core.macros.RsExpandedElementUtil;
import org.rust.lang.core.psi.*;
import org.rust.lang.core.psi.ext.RsElement;
import org.rust.lang.core.psi.ext.PsiElementUtil;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class SearchByOffset {
    private SearchByOffset() {
    }

    @Nullable
    public static RsExpr findExpressionAtCaret(@NotNull RsFile file, int offset) {
        RsExpr expr = expressionAtOffset(file, offset);
        RsExpr exprBefore = expressionAtOffset(file, offset - 1);
        if (expr == null) return exprBefore;
        if (exprBefore == null) return expr;
        if (PsiTreeUtil.isAncestor(expr, exprBefore, false)) return exprBefore;
        return expr;
    }

    @Nullable
    private static RsExpr expressionAtOffset(@NotNull RsFile file, int offset) {
        PsiElement element = file.findElementAt(offset);
        if (element == null) return null;
        return PsiTreeUtil.getParentOfType(element, RsExpr.class, false);
    }

    @Nullable
    public static RsExpr findExpressionInRange(@NotNull PsiFile file, int startOffset, int endOffset) {
        PsiElement[] range = getElementRange(file, startOffset, endOffset);
        if (range == null) return null;
        PsiElement element1 = range[0];
        PsiElement element2 = range[1];

        PsiElement parent = PsiTreeUtil.findCommonParent(element1, element2);
        if (parent == null) return null;
        parent = PsiTreeUtil.getParentOfType(parent, RsExpr.class, false);
        if (parent == null) return null;

        if (element1 == PsiTreeUtil.getDeepestFirst(parent) && element2 == PsiTreeUtil.getDeepestLast(element2)) {
            return (RsExpr) parent;
        }

        return null;
    }

    @NotNull
    public static PsiElement[] findStatementsInRange(@NotNull PsiFile file, int startOffset, int endOffset) {
        PsiElement[] range = getElementRange(file, startOffset, endOffset);
        if (range == null) return PsiElement.EMPTY_ARRAY;
        return findStatementsInRange(range[0], range[1]);
    }

    @NotNull
    public static PsiElement[] findStatementsOrExprInRange(@NotNull PsiFile file, int startOffset, int endOffset) {
        PsiElement[] range = getElementRange(file, startOffset, endOffset);
        if (range == null) return PsiElement.EMPTY_ARRAY;
        PsiElement element1 = range[0];
        PsiElement element2 = range[1];
        PsiElement parent = PsiTreeUtil.findCommonParent(element1, element2);
        if (parent == null) return PsiElement.EMPTY_ARRAY;

        if (startOffset == parent.getTextOffset() && endOffset == parent.getTextOffset() + parent.getTextLength()) {
            PsiElement mostDistantParent = null;
            PsiElement current = parent;
            while (current != null) {
                if (current.getTextRange().equals(parent.getTextRange()) && (current instanceof RsExpr || current instanceof RsStmt)) {
                    mostDistantParent = current;
                }
                current = current.getParent();
                if (current != null && !current.getTextRange().equals(parent.getTextRange())) {
                    break;
                }
            }
            if (mostDistantParent != null) {
                return new PsiElement[]{mostDistantParent};
            }
        }

        return findStatementsInRange(element1, element2);
    }

    @NotNull
    private static PsiElement[] findStatementsInRange(@NotNull PsiElement element1, @NotNull PsiElement element2) {
        PsiElement parent = PsiTreeUtil.findCommonParent(element1, element2);
        RsBlock block = PsiTreeUtil.getParentOfType(parent, RsBlock.class, false);
        if (block == null) return PsiElement.EMPTY_ARRAY;

        int realStartOffset = element1.getTextOffset();
        int realEndOffset = element2.getTextOffset() + element2.getTextLength();

        PsiElement checkedElement1 = getTopmostParentInside(element1, block);
        if (realStartOffset != checkedElement1.getTextOffset()) return PsiElement.EMPTY_ARRAY;

        PsiElement checkedElement2 = getTopmostParentInside(element2, block);
        if (realEndOffset != checkedElement2.getTextOffset() + checkedElement2.getTextLength()) return PsiElement.EMPTY_ARRAY;

        return findStatementsInRangeUnchecked(checkedElement1, checkedElement2);
    }

    @NotNull
    private static PsiElement[] findStatementsInRangeUnchecked(@NotNull PsiElement element1, @NotNull PsiElement element2) {
        List<PsiElement> elements = collectElements(element1, element2.getNextSibling());

        for (int idx = 0; idx < elements.size(); idx++) {
            PsiElement element = elements.get(idx);
            boolean isValid = element instanceof RsStmt
                || (element instanceof RsMacroCall && RsExpandedElementUtil.getExpansionContext((RsMacroCall) element) == MacroExpansionContext.STMT)
                || (idx == elements.size() - 1 && element instanceof RsExpr)
                || element instanceof PsiComment;
            if (!isValid) return PsiElement.EMPTY_ARRAY;
        }

        return PsiUtilCore.toPsiElementArray(elements);
    }

    @Nullable
    public static PsiElement[] getElementRange(@NotNull PsiFile file, int startOffset, int endOffset) {
        PsiElement element1 = findElementAtIgnoreWhitespaceBefore(file, startOffset);
        PsiElement element2 = findElementAtIgnoreWhitespaceAfter(file, endOffset - 1);
        if (element1 == null || element2 == null) return null;

        if (element1.getTextOffset() >= element2.getTextOffset() + element2.getTextLength()) return null;

        return new PsiElement[]{element1, element2};
    }

    @Nullable
    public static PsiElement findElementAtIgnoreWhitespaceBefore(@NotNull PsiFile file, int offset) {
        PsiElement element = file.findElementAt(offset);
        if (element instanceof PsiWhiteSpace) {
            return file.findElementAt(element.getTextRange().getEndOffset());
        }
        return element;
    }

    @Nullable
    public static PsiElement findElementAtIgnoreWhitespaceAfter(@NotNull PsiFile file, int offset) {
        PsiElement element = file.findElementAt(offset);
        if (element instanceof PsiWhiteSpace) {
            return file.findElementAt(element.getTextRange().getStartOffset() - 1);
        }
        return element;
    }

    @NotNull
    public static PsiElement getTopmostParentInside(@NotNull PsiElement element, @NotNull PsiElement parent) {
        if (parent == element) return element;

        PsiElement current = element;
        while (parent != current.getParent()) {
            current = current.getParent();
        }
        return current;
    }

    @NotNull
    public static List<PsiElement> collectElements(@NotNull PsiElement start, @Nullable PsiElement stop) {
        List<PsiElement> result = new ArrayList<>();
        PsiElement current = start;
        while (current != null && current != stop) {
            if (!(current instanceof PsiWhiteSpace)) {
                result.add(current);
            }
            current = current.getNextSibling();
        }
        return result;
    }
}
