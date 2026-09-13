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
import org.rust.lang.core.psi.RsForExpr;
import org.rust.lang.core.psi.impl.RsPsiFactory;
import org.rust.lang.core.psi.ext.impl.RsFunctionUtil;
import consulo.localize.LocalizeValue;

public class RsWithForSurrounder extends RsStatementsSurrounderBase.BlockWithCondition<RsForExpr> {

    @Override
    public consulo.localize.LocalizeValue getTemplateDescription() {
        return consulo.localize.LocalizeValue.of(RsBundle.message("action.for.text"));
    }

    @Override
    protected Pair<RsForExpr, RsBlock> createTemplate(Project project) {
        RsForExpr forExpr = (RsForExpr) new RsPsiFactory(project).createExpression("for a in b {}");
        return Pair.create(forExpr, forExpr.getBlock());
    }

    @Override
    protected TextRange conditionRange(RsForExpr expression) {
        return new TextRange(
            expression.getPat().getTextOffset(),
            expression.getExpr().getTextRange().getEndOffset()
        );
    }
}
