/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.refactoring;

import org.rust.lang.core.psi.ext.RsElementUtil;
import consulo.language.editor.PsiEquivalenceUtil;
import consulo.codeEditor.Editor;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiRecursiveElementVisitor;
import consulo.language.psi.util.PsiTreeUtil;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
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

    @Nonnull
    public static List<RsExpr> findCandidateExpressionsToExtract(@Nonnull Editor editor, @Nonnull RsFile file) {
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

    @Nonnull
    public static List<RsExpr> findOccurrences(@Nonnull RsExpr expr) {
        RsElement parent = RsElementUtil.ancestorOrSelf(expr, RsBlock.class);
        if (parent == null) {
            parent = RsElementUtil.ancestorOrSelf(expr, RsItemElement.class);
        }
        if (parent == null) {
            return Collections.emptyList();
        }
        return findOccurrences(parent, expr);
    }

    @Nonnull
    public static List<RsExpr> findOccurrences(@Nonnull RsElement parent, @Nonnull RsExpr expr) {
        List<RsExpr> foundOccurrences = new ArrayList<>();
        PsiRecursiveElementVisitor visitor = new PsiRecursiveElementVisitor() {
            @Override
            public void visitElement(@Nonnull PsiElement element) {
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
    public static RsPatBinding moveEditorToNameElement(@Nonnull Editor editor, @Nullable PsiElement element) {
        RsPatBinding newName = element != null ? findBinding(element) : null;
        int offset = 0;
        if (newName != null && newName.getIdentifier() != null && newName.getIdentifier().getTextRange() != null) {
            offset = newName.getIdentifier().getTextRange().getStartOffset();
        }
        editor.getCaretModel().moveToOffset(offset);
        return newName;
    }

    @Nullable
    public static RsPatBinding findBinding(@Nonnull PsiElement element) {
        return PsiTreeUtil.findChildOfType(element, RsPatBinding.class);
    }
}
