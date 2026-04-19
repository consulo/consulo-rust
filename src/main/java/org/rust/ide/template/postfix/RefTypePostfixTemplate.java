/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.template.postfix;

import com.intellij.codeInsight.template.postfix.templates.StringBasedPostfixTemplate;
import com.intellij.psi.PsiElement;

public class RefTypePostfixTemplate extends StringBasedPostfixTemplate {
    public RefTypePostfixTemplate(RsPostfixTemplateProvider provider) {
        super("ref", "&type", new RsTypeParentsSelector(), provider);
    }

    @Override
    public String getTemplateString(PsiElement element) {
        return "&" + element.getText();
    }

    @Override
    public PsiElement getElementToRemove(PsiElement expr) {
        return expr;
    }
}
