/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.template.postfix;

import consulo.language.editor.refactoring.postfixTemplate.PostfixTemplateWithExpressionSelector;
import consulo.codeEditor.Editor;
import consulo.language.psi.PsiElement;
import org.rust.RsBundle;
import org.rust.ide.refactoring.introduceVariable.IntroduceVariableImpl;
import org.rust.lang.core.psi.RsExpr;

public class LetPostfixTemplate extends PostfixTemplateWithExpressionSelector {

    public LetPostfixTemplate(RsPostfixTemplateProvider provider) {
        super(null, "let", "let name = expr;", new RsExprParentsSelector(), provider);
    }

    @Override
    protected void expandForChooseExpression(PsiElement expression, Editor editor) {
        if (!(expression instanceof RsExpr)) return;
        IntroduceVariableImpl.extractExpression(
            editor, (RsExpr) expression, true, RsBundle.message("command.name.postfix.let.template")
        );
    }
}
