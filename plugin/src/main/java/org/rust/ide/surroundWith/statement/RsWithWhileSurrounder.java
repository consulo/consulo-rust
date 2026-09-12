/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.surroundWith.statement;

import consulo.project.Project;
import consulo.util.lang.Pair;
import consulo.document.util.TextRange;
import org.rust.RsBundle;
import org.rust.lang.core.psi.RsBlock;
import org.rust.lang.core.psi.RsPsiFactory;
import org.rust.lang.core.psi.RsWhileExpr;
import org.rust.lang.core.psi.ext.RsFunctionUtil;
import consulo.localize.LocalizeValue;

public class RsWithWhileSurrounder extends RsStatementsSurrounderBase.BlockWithCondition<RsWhileExpr> {

    @Override
    public consulo.localize.LocalizeValue getTemplateDescription() {
        return consulo.localize.LocalizeValue.of(RsBundle.message("action.while.text"));
    }

    @Override
    protected Pair<RsWhileExpr, RsBlock> createTemplate(Project project) {
        RsWhileExpr whileExpr = (RsWhileExpr) new RsPsiFactory(project).createExpression("while a {}");
        return Pair.create(whileExpr, whileExpr.getBlock());
    }

    @Override
    protected TextRange conditionRange(RsWhileExpr expression) {
        return expression.getCondition().getTextRange();
    }
}
