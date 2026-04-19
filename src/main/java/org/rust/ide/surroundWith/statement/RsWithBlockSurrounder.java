/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.surroundWith.statement;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Pair;
import org.rust.lang.core.psi.RsBlock;
import org.rust.lang.core.psi.RsBlockExpr;
import org.rust.lang.core.psi.RsPsiFactory;
import org.rust.lang.core.psi.ext.RsFunctionUtil;

public class RsWithBlockSurrounder extends RsStatementsSurrounderBase.SimpleBlock<RsBlockExpr> {

    @Override
    public String getTemplateDescription() {
        return "{}";
    }

    @Override
    protected Pair<RsBlockExpr, RsBlock> createTemplate(Project project) {
        RsBlockExpr block = new RsPsiFactory(project).createBlockExpr("");
        return Pair.create(block, block.getBlock());
    }
}
