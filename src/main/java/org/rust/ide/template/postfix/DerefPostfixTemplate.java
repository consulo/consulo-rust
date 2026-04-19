/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.template.postfix;

import com.intellij.codeInsight.template.postfix.templates.StringBasedPostfixTemplate;
import com.intellij.psi.PsiElement;
import org.rust.lang.core.types.RsTypesUtil;
import org.rust.lang.core.types.ty.TyPointer;
import org.rust.lang.core.types.ty.TyReference;

public class DerefPostfixTemplate extends StringBasedPostfixTemplate {
    public DerefPostfixTemplate(RsPostfixTemplateProvider provider) {
        super("deref", "*expr",
            new RsExprParentsSelector(expr -> {
                org.rust.lang.core.types.ty.Ty ty = RsTypesUtil.getType(expr);
                return ty instanceof TyReference || ty instanceof TyPointer;
            }), provider);
    }

    @Override
    public String getTemplateString(PsiElement element) {
        return "*" + element.getText();
    }

    @Override
    public PsiElement getElementToRemove(PsiElement expr) {
        return expr;
    }
}
