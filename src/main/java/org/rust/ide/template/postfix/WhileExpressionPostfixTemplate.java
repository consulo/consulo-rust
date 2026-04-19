/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.template.postfix;

import com.intellij.codeInsight.template.postfix.templates.PostfixTemplateProvider;
import com.intellij.codeInsight.template.postfix.templates.SurroundPostfixTemplateBase;
import com.intellij.lang.surroundWith.Surrounder;
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
