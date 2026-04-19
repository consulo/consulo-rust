/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.template.postfix;

import com.intellij.codeInsight.template.postfix.templates.PostfixTemplateWithExpressionSelector;
import com.intellij.openapi.editor.Editor;
import com.intellij.psi.PsiElement;
import org.rust.lang.core.psi.*;
import org.rust.lang.core.psi.ext.*;
import org.rust.lang.core.resolve.KnownItems;
import org.rust.lang.core.types.RsTypesUtil;
import org.rust.lang.core.types.TraitRef;
import org.rust.lang.core.types.RsImplLookupUtil;
import org.rust.lang.core.types.ty.TyUnit;

import java.util.function.Predicate;

public class PrintlnPostfixTemplate extends PostfixTemplateWithExpressionSelector {

    private final String myMacroName;

    public PrintlnPostfixTemplate(RsPostfixTemplateProvider provider) {
        this(provider, "println");
    }

    public PrintlnPostfixTemplate(RsPostfixTemplateProvider provider, String macroName) {
        super(null, macroName, macroName + "!(\"{:?}\", expr);",
            new RsExprParentsSelector(expr -> isNotIgnored(expr) && (isDebug(expr) || isDisplay(expr))),
            provider);
        myMacroName = macroName;
    }

    @Override
    protected void expandForChooseExpression(PsiElement expression, Editor editor) {
        if (!(expression instanceof RsExpr)) return;
        RsExpr rsExpr = (RsExpr) expression;

        RsPsiFactory psiFactory = new RsPsiFactory(expression.getProject());
        String fmt = getFmt(rsExpr);
        String macroStart = myMacroName + "!(";

        PsiElement parent = expression.getParent();
        if (parent instanceof RsLetDecl) {
            RsLetDecl letDecl = (RsLetDecl) parent;
            RsPat pat = letDecl.getPat();
            if (pat == null) return;
            String expressionText = pat.getText();
            RsExpr macro = createMacro(psiFactory, macroStart, fmt, expressionText, true);
            // Add macro after the let statement
            PsiElement newElement = parent.getParent().addAfter(macro, getNextNonComment(parent));
            editor.getCaretModel().moveToOffset(newElement.getTextRange().getEndOffset());
            if (letDecl.getSemicolon() == null) {
                parent.add(psiFactory.createSemicolon());
            }
        } else {
            boolean isTail = RsExprUtil.isTailExpr(rsExpr);
            RsExpr macro = createMacro(psiFactory, macroStart, fmt, expression.getText(), isTail);
            PsiElement newElement = expression.replace(macro);
            editor.getCaretModel().moveToOffset(newElement.getTextRange().getEndOffset());
        }
    }

    private static String getFmt(RsExpr expr) {
        if (expr instanceof RsLitExpr) {
            RsLiteralKind kind = RsLiteralKindUtil.getKind((RsLitExpr) expr);
            if (kind instanceof RsLiteralKind.StringLiteral && !((RsLiteralKind.StringLiteral) kind).isByte()) {
                return "";
            }
        }
        if (isDisplay(expr)) return "{}";
        return "{:?}";
    }

    private static RsExpr createMacro(RsPsiFactory psiFactory, String macroStart, String fmt, String expressionText, boolean addTrailingSemicolon) {
        RsExpr macroExpression;
        if (fmt.isEmpty()) {
            macroExpression = psiFactory.createExpression(macroStart + expressionText + ")");
        } else {
            macroExpression = psiFactory.createExpression(macroStart + "\"" + fmt + "\", " + expressionText + ")");
        }
        if (addTrailingSemicolon) {
            macroExpression.add(psiFactory.createSemicolon());
        }
        return macroExpression;
    }

    private static PsiElement getNextNonComment(PsiElement element) {
        PsiElement sibling = element.getNextSibling();
        while (sibling != null && sibling instanceof com.intellij.psi.PsiWhiteSpace) {
            sibling = sibling.getNextSibling();
        }
        return sibling != null ? sibling.getPrevSibling() : element;
    }

    private static boolean isNotIgnored(RsExpr expr) {
        PsiElement parent = expr.getParent();
        if (parent == null) return false;
        if (parent instanceof RsLetDecl) return !(((RsLetDecl) parent).getPat() instanceof RsPatWild);
        return parent instanceof RsMatchArm || parent instanceof RsStmt
            || parent instanceof RsBlock || parent instanceof RsBinaryExpr;
    }

    private static boolean isDebug(RsExpr expr) {
        org.rust.lang.core.resolve.KnownItems items = KnownItems.getKnownItems(expr);
        if (items.getDebug() == null) return false;
        return true; // simplified
    }

    private static boolean isDisplay(RsExpr expr) {
        org.rust.lang.core.resolve.KnownItems items = KnownItems.getKnownItems(expr);
        if (items.getDisplay() == null) return false;
        return true; // simplified
    }
}
