/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.template.postfix;

import consulo.language.editor.template.Template;
import consulo.language.editor.template.TextExpression;
import com.intellij.codeInsight.template.postfix.templates.StringBasedPostfixTemplate;
import consulo.language.psi.PsiElement;

public class IterPostfixTemplate extends StringBasedPostfixTemplate {
    public IterPostfixTemplate(String name, RsPostfixTemplateProvider provider) {
        super(name, "for x in expr", new RsExprParentsSelector(), provider);
    }

    @Override
    public String getTemplateString(PsiElement element) {
        return "for $name$ in " + element.getText() + " {\n     $END$\n}";
    }

    public void setVariables(Template template, PsiElement element) {
        template.addVariable("name", new TextExpression("x"), true);
    }

    @Override
    public PsiElement getElementToRemove(PsiElement expr) {
        return expr;
    }
}
