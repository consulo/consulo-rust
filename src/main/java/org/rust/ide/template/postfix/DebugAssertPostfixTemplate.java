/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.template.postfix;

import com.intellij.codeInsight.template.postfix.templates.StringBasedPostfixTemplate;
import com.intellij.psi.PsiElement;
import org.rust.lang.core.psi.RsBinaryExpr;
import org.rust.lang.core.psi.RsExpr;
import org.rust.lang.core.psi.ext.RsBinaryOpUtil;
import org.rust.lang.core.psi.ext.EqualityOp;

public class DebugAssertPostfixTemplate extends StringBasedPostfixTemplate {

    public DebugAssertPostfixTemplate(RsPostfixTemplateProvider provider) {
        super("debug_assert", "debug_assert!(exp);",
            new RsExprParentsSelector(RsPostfixTemplateUtils::isBool), provider);
    }

    @Override
    public String getTemplateString(PsiElement element) {
        if (element instanceof RsBinaryExpr) {
            RsBinaryExpr binaryExpr = (RsBinaryExpr) element;
            if (RsBinaryOpUtil.getOperatorType(binaryExpr) == EqualityOp.EQ) {
                RsExpr right = binaryExpr.getRight();
                return "debug_assert_eq!(" + binaryExpr.getLeft().getText() + ", " + (right != null ? right.getText() : "") + ");$END$";
            }
        }
        return "debug_assert!(" + element.getText() + ");$END$";
    }

    @Override
    public PsiElement getElementToRemove(PsiElement expr) {
        return expr;
    }
}
