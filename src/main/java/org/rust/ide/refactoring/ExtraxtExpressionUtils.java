/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.refactoring;

import org.rust.lang.core.psi.ext.RsElementUtil;
import com.intellij.codeInsight.PsiEquivalenceUtil;
import com.intellij.openapi.editor.Editor;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiRecursiveElementVisitor;
import com.intellij.psi.util.PsiTreeUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.rust.ide.utils.ExpressionUtils;
import org.rust.lang.core.psi.*;
import org.rust.lang.core.psi.ext.RsElement;
import org.rust.lang.core.psi.ext.RsItemElement;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class ExtraxtExpressionUtils {
    private ExtraxtExpressionUtils() {
    }

    @NotNull
    public static List<RsExpr> findCandidateExpressionsToExtract(@NotNull Editor editor, @NotNull RsFile file) {
        if (editor.getSelectionModel().hasSelection()) {
            RsExpr expr = org.rust.ide.utils.SearchByOffset.findExpressionInRange(
                file,
                editor.getSelectionModel().getSelectionStart(),
                editor.getSelectionModel().getSelectionEnd()
            );
            if (expr != null) {
                return Collections.singletonList(expr);
            }
            return Collections.emptyList();
        } else {
            RsExpr expr = org.rust.ide.utils.SearchByOffset.findExpressionAtCaret(file, editor.getCaretModel().getOffset());
            if (expr == null) {
                return Collections.emptyList();
            }
            List<RsExpr> result = new ArrayList<>();
            PsiElement current = expr;
            while (current != null && !(current instanceof RsBlock)) {
                if (current instanceof RsExpr && !(current instanceof RsPathExpr)) {
                    result.add((RsExpr) current);
                }
                current = current.getParent();
            }
            return result;
        }
    }

    @NotNull
    public static List<RsExpr> findOccurrences(@NotNull RsExpr expr) {
        RsElement parent = RsElementUtil.ancestorOrSelf(expr, RsBlock.class);
        if (parent == null) {
            parent = RsElementUtil.ancestorOrSelf(expr, RsItemElement.class);
        }
        if (parent == null) {
            return Collections.emptyList();
        }
        return findOccurrences(parent, expr);
    }

    @NotNull
    public static List<RsExpr> findOccurrences(@NotNull RsElement parent, @NotNull RsExpr expr) {
        List<RsExpr> foundOccurrences = new ArrayList<>();
        PsiRecursiveElementVisitor visitor = new PsiRecursiveElementVisitor() {
            @Override
            public void visitElement(@NotNull PsiElement element) {
                if (element instanceof RsExpr && PsiEquivalenceUtil.areElementsEquivalent(expr, element)) {
                    foundOccurrences.add((RsExpr) element);
                } else {
                    super.visitElement(element);
                }
            }
        };
        parent.acceptChildren(visitor);
        return foundOccurrences;
    }

    @Nullable
    public static RsPatBinding moveEditorToNameElement(@NotNull Editor editor, @Nullable PsiElement element) {
        RsPatBinding newName = element != null ? findBinding(element) : null;
        int offset = 0;
        if (newName != null && newName.getIdentifier() != null && newName.getIdentifier().getTextRange() != null) {
            offset = newName.getIdentifier().getTextRange().getStartOffset();
        }
        editor.getCaretModel().moveToOffset(offset);
        return newName;
    }

    @Nullable
    public static RsPatBinding findBinding(@NotNull PsiElement element) {
        return PsiTreeUtil.findChildOfType(element, RsPatBinding.class);
    }
}
