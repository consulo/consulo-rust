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
import org.rust.lang.core.psi.RsIfExpr;
import org.rust.lang.core.psi.RsPsiFactory;
import org.rust.lang.core.psi.ext.RsFunctionUtil;

public class RsWithIfSurrounder extends RsStatementsSurrounderBase.BlockWithCondition<RsIfExpr> {

    @Override
    public String getTemplateDescription() {
        return RsBundle.message("action.if.text");
    }

    @Override
    protected Pair<RsIfExpr, RsBlock> createTemplate(Project project) {
        RsIfExpr ifExpr = (RsIfExpr) new RsPsiFactory(project).createExpression("if a {}");
        return Pair.create(ifExpr, ifExpr.getBlock());
    }

    @Override
    protected TextRange conditionRange(RsIfExpr expression) {
        return expression.getCondition().getTextRange();
    }
}
