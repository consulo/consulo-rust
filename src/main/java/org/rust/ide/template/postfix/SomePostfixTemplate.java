/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.template.postfix;

import com.intellij.codeInsight.template.postfix.templates.StringBasedPostfixTemplate;
import com.intellij.psi.PsiElement;

public class SomePostfixTemplate extends StringBasedPostfixTemplate {
    public SomePostfixTemplate(RsPostfixTemplateProvider provider) {
        super("some", "Some(expr)", new RsExprParentsSelector(), provider);
    }

    @Override
    public String getTemplateString(PsiElement element) {
        return "Some(" + element.getText() + ")";
    }

    @Override
    public PsiElement getElementToRemove(PsiElement expr) {
        return expr;
    }
}
