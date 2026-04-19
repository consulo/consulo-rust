/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.fixes;

import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.rust.RsBundle;
import org.rust.lang.core.psi.*;
import org.rust.lang.core.psi.RsElementTypes;
import org.rust.lang.core.psi.ext.RsOuterAttributeOwner;

import java.util.List;

public class ReplaceIncDecOperatorFix extends RsQuickFixBase<PsiElement> {

    private final String replacement;

    private ReplaceIncDecOperatorFix(@NotNull PsiElement operator, @NotNull String replacement) {
        super(operator);
        this.replacement = replacement;
    }

    @NotNull
    @Override
    public String getFamilyName() {
        return getText();
    }

    @NotNull
    @Override
    public String getText() {
        return RsBundle.message("intention.name.replace.inc.dec.operator", replacement);
    }

    @Override
    public void invoke(@NotNull Project project, @Nullable Editor editor, @NotNull PsiElement element) {
        PsiElement parent = element.getParent();
        RsExpr subexpr = null;
        if (parent instanceof RsPrefixIncExpr) {
            subexpr = ((RsPrefixIncExpr) parent).getExpr();
        } else if (parent instanceof RsPostfixIncExpr) {
            subexpr = ((RsPostfixIncExpr) parent).getExpr();
        } else if (parent instanceof RsPostfixDecExpr) {
            subexpr = ((RsPostfixDecExpr) parent).getExpr();
        } else if (parent instanceof RsUnaryExpr) {
            RsExpr innerExpr = ((RsUnaryExpr) parent).getExpr();
            if (innerExpr instanceof RsUnaryExpr) {
                subexpr = ((RsUnaryExpr) innerExpr).getExpr();
            }
        }
        if (subexpr == null) return;
        RsExpr newExpr = new RsPsiFactory(project).createExpression(subexpr.getText() + " " + replacement);
        parent.replace(newExpr);
    }

    @Nullable
    public static ReplaceIncDecOperatorFix create(@NotNull PsiElement operator) {
        if (!(operator instanceof RsInc) && !(operator instanceof RsDec)
            && operator.getNode().getElementType() != RsElementTypes.MINUS) {
            return null;
        }

        PsiElement parent = operator.getParent();
        if (!(parent instanceof RsExpr)) return null;

        boolean isApplicable = false;
        if (parent instanceof RsPrefixIncExpr) {
            isApplicable = operator instanceof RsInc && ((RsPrefixIncExpr) parent).getExpr() != null;
        } else if (parent instanceof RsPostfixIncExpr) {
            isApplicable = operator instanceof RsInc;
        } else if (parent instanceof RsPostfixDecExpr) {
            isApplicable = operator instanceof RsDec;
        } else if (parent instanceof RsUnaryExpr) {
            isApplicable = isNegation(parent) && isNegation(((RsUnaryExpr) parent).getExpr());
        }

        if (!isApplicable) return null;

        if (!(parent.getParent() instanceof RsExprStmt)) return null;

        // Check for outer attributes
        List<RsOuterAttr> outerAttrList;
        if (parent.getParent() instanceof RsExprStmt) {
            outerAttrList = ((RsExprStmt) parent.getParent()).getOuterAttrList();
        } else if (parent instanceof RsPrefixIncExpr) {
            outerAttrList = ((RsPrefixIncExpr) parent).getOuterAttrList();
        } else if (parent instanceof RsUnaryExpr) {
            outerAttrList = ((RsUnaryExpr) parent).getOuterAttrList();
        } else {
            outerAttrList = java.util.Collections.emptyList();
        }
        if (outerAttrList != null && !outerAttrList.isEmpty()) return null;

        String replacement;
        if (operator instanceof RsInc) {
            replacement = "+= 1";
        } else if (operator instanceof RsDec || isNegation(parent)) {
            replacement = "-= 1";
        } else {
            return null;
        }

        return new ReplaceIncDecOperatorFix(operator, replacement);
    }

    private static boolean isNegation(@Nullable PsiElement element) {
        return element instanceof RsUnaryExpr && ((RsUnaryExpr) element).getMinus() != null;
    }
}
