/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.surroundWith.statement;

import consulo.project.Project;
import consulo.util.lang.Pair;
import org.rust.lang.core.psi.RsBlock;
import org.rust.lang.core.psi.RsBlockExpr;
import org.rust.lang.core.psi.impl.RsPsiFactory;
import org.rust.lang.core.psi.ext.impl.RsFunctionUtil;
import consulo.localize.LocalizeValue;

public class RsWithBlockSurrounder extends RsStatementsSurrounderBase.SimpleBlock<RsBlockExpr> {

    @Override
    public consulo.localize.LocalizeValue getTemplateDescription() {
        return consulo.localize.LocalizeValue.of("{}");
    }

    @Override
    protected Pair<RsBlockExpr, RsBlock> createTemplate(Project project) {
        RsBlockExpr block = new RsPsiFactory(project).createBlockExpr("");
        return Pair.create(block, block.getBlock());
    }
}
