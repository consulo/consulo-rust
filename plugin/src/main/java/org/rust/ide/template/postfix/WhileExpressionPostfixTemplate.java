/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.template.postfix;

import consulo.language.editor.postfixTemplate.PostfixTemplateProvider;
import consulo.language.editor.refactoring.postfixTemplate.SurroundPostfixTemplateBase;
import consulo.language.editor.surroundWith.Surrounder;
import org.rust.ide.surroundWith.expression.RsWithWhileExpSurrounder;

public class WhileExpressionPostfixTemplate extends SurroundPostfixTemplateBase {
    public WhileExpressionPostfixTemplate(PostfixTemplateProvider provider) {
        super("while", "while exp {}", PostfixUtil.RsPostfixTemplatePsiInfo,
            new RsExprParentsSelector(RsPostfixTemplateUtils::isBool), provider);
    }

    @Override
    protected Surrounder getSurrounder() {
        return new RsWithWhileExpSurrounder();
    }
}
