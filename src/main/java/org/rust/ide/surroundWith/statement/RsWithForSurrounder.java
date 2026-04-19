/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.surroundWith.statement;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Pair;
import com.intellij.openapi.util.TextRange;
import org.rust.RsBundle;
import org.rust.lang.core.psi.RsBlock;
import org.rust.lang.core.psi.RsForExpr;
import org.rust.lang.core.psi.RsPsiFactory;
import org.rust.lang.core.psi.ext.RsFunctionUtil;

public class RsWithForSurrounder extends RsStatementsSurrounderBase.BlockWithCondition<RsForExpr> {

    @Override
    public String getTemplateDescription() {
        return RsBundle.message("action.for.text");
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
