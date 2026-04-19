/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.template.postfix;

import com.intellij.codeInsight.template.Template;
import com.intellij.codeInsight.template.impl.TextExpression;
import com.intellij.codeInsight.template.postfix.templates.StringBasedPostfixTemplate;
import com.intellij.psi.PsiElement;

public class IterPostfixTemplate extends StringBasedPostfixTemplate {
    public IterPostfixTemplate(String name, RsPostfixTemplateProvider provider) {
        super(name, "for x in expr", new RsExprParentsSelector(), provider);
    }

    @Override
    public String getTemplateString(PsiElement element) {
        return "for $name$ in " + element.getText() + " {\n     $END$\n}";
    }

    @Override
    public void setVariables(Template template, PsiElement element) {
        template.addVariable("name", new TextExpression("x"), true);
    }

    @Override
    public PsiElement getElementToRemove(PsiElement expr) {
        return expr;
    }
}
