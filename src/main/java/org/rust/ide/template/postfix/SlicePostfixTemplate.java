/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.template.postfix;

import com.intellij.codeInsight.template.Template;
import com.intellij.codeInsight.template.impl.TextExpression;
import com.intellij.codeInsight.template.postfix.templates.StringBasedPostfixTemplate;
import com.intellij.psi.PsiElement;
import org.rust.lang.core.psi.RsUnaryExpr;

public class SlicePostfixTemplate extends StringBasedPostfixTemplate {
    public SlicePostfixTemplate(String name, RsPostfixTemplateProvider provider) {
        super(name, "&list[i..j]", new RsExprParentsSelector(), provider);
    }

    @Override
    public String getTemplateString(PsiElement element) {
        boolean isRef = element instanceof RsUnaryExpr; // simplified
        String prefix = isRef ? "" : "&";
        return prefix + element.getText() + "[$from$..$to$]$END$";
    }

    @Override
    public void setVariables(Template template, PsiElement element) {
        template.addVariable("from", new TextExpression("i"), true);
        template.addVariable("to", new TextExpression("j"), true);
    }

    @Override
    public PsiElement getElementToRemove(PsiElement expr) {
        return expr;
    }
}
