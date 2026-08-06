/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.fixes;

import consulo.codeEditor.Editor;
import consulo.project.Project;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.rust.RsBundle;
import org.rust.lang.core.psi.RsBlockExpr;
import org.rust.lang.core.psi.RsLabelDecl;
import org.rust.lang.core.psi.RsPsiFactory;
import org.rust.lang.core.psi.ext.RsFunctionUtil;

public class RsConvertBlockToLoopFix extends RsQuickFixBase<RsBlockExpr> {

    public RsConvertBlockToLoopFix(@Nonnull RsBlockExpr element) {
        super(element);
    }

    @Nonnull
        public consulo.localize.LocalizeValue getFamilyName() {
        return consulo.localize.LocalizeValue.of(RsBundle.message("intention.family.name.convert.to.loop"));
    }

    @Nonnull
    @Override
    public consulo.localize.LocalizeValue getText() {
        return getFamilyName();
    }

    @Override
    public void invoke(@Nonnull Project project, @Nullable Editor editor, @Nonnull RsBlockExpr element) {
        RsLabelDecl labelDecl = element.getLabelDecl();
        if (labelDecl == null) return;
        String labelName = labelDecl.getName();
        if (labelName == null) return;
        element.replace(new RsPsiFactory(project).createLoop(element.getBlock().getText(), labelName));
    }
}
