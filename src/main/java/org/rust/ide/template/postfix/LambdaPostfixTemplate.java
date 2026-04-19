/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.template.postfix;

import com.intellij.codeInsight.template.postfix.templates.StringBasedPostfixTemplate;
import com.intellij.psi.PsiElement;

public class LambdaPostfixTemplate extends StringBasedPostfixTemplate {
    public LambdaPostfixTemplate(RsPostfixTemplateProvider provider) {
        super("lambda", "|| expr", new RsExprParentsSelector(), provider);
    }

    @Override
    public String getTemplateString(PsiElement element) {
        return "|| " + element.getText();
    }

    @Override
    public PsiElement getElementToRemove(PsiElement expr) {
        return expr;
    }
}
