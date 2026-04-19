/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.template.postfix;

import com.intellij.codeInsight.template.postfix.templates.PostfixTemplateProvider;
import com.intellij.codeInsight.template.postfix.templates.SurroundPostfixTemplateBase;
import com.intellij.lang.surroundWith.Surrounder;
import com.intellij.psi.PsiElement;
import org.rust.ide.surroundWith.expression.RsWithIfExpSurrounder;
import org.rust.lang.utils.RsNegateUtil;
import org.rust.lang.utils.RsBooleanExpUtils;

public class ElseExpressionPostfixTemplate extends SurroundPostfixTemplateBase {
    public ElseExpressionPostfixTemplate(PostfixTemplateProvider provider) {
        super("else", "if !exp {}", PostfixUtil.RsPostfixTemplatePsiInfo,
            new RsExprParentsSelector(RsPostfixTemplateUtils::isBool), provider);
    }

    @Override
    protected Surrounder getSurrounder() {
        return new RsWithIfExpSurrounder();
    }

    @Override
    protected PsiElement getWrappedExpression(PsiElement expression) {
        return RsBooleanExpUtils.negate(expression);
    }
}
