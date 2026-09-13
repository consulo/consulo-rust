/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.surroundWith.statement;

import consulo.project.Project;
import consulo.util.lang.Pair;
import org.rust.RsBundle;
import org.rust.lang.core.psi.RsBlock;
import org.rust.lang.core.psi.RsLoopExpr;
import org.rust.lang.core.psi.impl.RsPsiFactory;
import org.rust.lang.core.psi.ext.impl.RsFunctionUtil;
import consulo.localize.LocalizeValue;

public class RsWithLoopSurrounder extends RsStatementsSurrounderBase.SimpleBlock<RsLoopExpr> {

    @Override
    public consulo.localize.LocalizeValue getTemplateDescription() {
        return consulo.localize.LocalizeValue.of(RsBundle.message("action.loop.text"));
    }

    @Override
    protected Pair<RsLoopExpr, RsBlock> createTemplate(Project project) {
        RsLoopExpr loop = (RsLoopExpr) new RsPsiFactory(project).createExpression("loop {}");
        return Pair.create(loop, loop.getBlock());
    }
}
