/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.template.postfix;

import consulo.language.editor.postfixTemplate.PostfixTemplateProvider;
import consulo.language.editor.refactoring.postfixTemplate.SurroundPostfixTemplateBase;
import consulo.language.editor.surroundWith.Surrounder;
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
