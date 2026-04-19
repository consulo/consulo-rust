/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.surroundWith.statement;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Pair;
import org.rust.RsBundle;
import org.rust.lang.core.psi.RsBlock;
import org.rust.lang.core.psi.RsLoopExpr;
import org.rust.lang.core.psi.RsPsiFactory;
import org.rust.lang.core.psi.ext.RsFunctionUtil;

public class RsWithLoopSurrounder extends RsStatementsSurrounderBase.SimpleBlock<RsLoopExpr> {

    @Override
    public String getTemplateDescription() {
        return RsBundle.message("action.loop.text");
    }

    @Override
    protected Pair<RsLoopExpr, RsBlock> createTemplate(Project project) {
        RsLoopExpr loop = (RsLoopExpr) new RsPsiFactory(project).createExpression("loop {}");
        return Pair.create(loop, loop.getBlock());
    }
}
