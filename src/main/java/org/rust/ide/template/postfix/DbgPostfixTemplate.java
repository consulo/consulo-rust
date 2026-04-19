/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.template.postfix;

import com.intellij.codeInsight.template.postfix.templates.StringBasedPostfixTemplate;
import com.intellij.psi.PsiElement;

public class DbgPostfixTemplate extends StringBasedPostfixTemplate {
    public DbgPostfixTemplate(RsPostfixTemplateProvider provider) {
        super("dbg", "dbg!(expr)", new RsExprParentsSelector(), provider);
    }

    @Override
    public String getTemplateString(PsiElement element) {
        return "dbg!(" + element.getText() + ")";
    }

    @Override
    public PsiElement getElementToRemove(PsiElement expr) {
        return expr;
    }
}
