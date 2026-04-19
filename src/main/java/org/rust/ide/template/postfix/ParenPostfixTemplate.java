/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.template.postfix;

import com.intellij.codeInsight.template.postfix.templates.PostfixTemplateProvider;
import com.intellij.codeInsight.template.postfix.templates.SurroundPostfixTemplateBase;
import com.intellij.lang.surroundWith.Surrounder;
import org.rust.ide.surroundWith.expression.RsWithParenthesesSurrounder;

public class ParenPostfixTemplate extends SurroundPostfixTemplateBase {
    public ParenPostfixTemplate(PostfixTemplateProvider provider) {
        super("par", "(expr)", PostfixUtil.RsPostfixTemplatePsiInfo,
            new RsExprParentsSelector(), provider);
    }

    @Override
    protected Surrounder getSurrounder() {
        return new RsWithParenthesesSurrounder();
    }
}
