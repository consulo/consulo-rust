/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.fixes;

import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.rust.RsBundle;
import org.rust.lang.core.psi.RsBlockExpr;
import org.rust.lang.core.psi.RsLabelDecl;
import org.rust.lang.core.psi.RsPsiFactory;
import org.rust.lang.core.psi.ext.RsFunctionUtil;

public class RsConvertBlockToLoopFix extends RsQuickFixBase<RsBlockExpr> {

    public RsConvertBlockToLoopFix(@NotNull RsBlockExpr element) {
        super(element);
    }

    @NotNull
    @Override
    public String getFamilyName() {
        return RsBundle.message("intention.family.name.convert.to.loop");
    }

    @NotNull
    @Override
    public String getText() {
        return getFamilyName();
    }

    @Override
    public void invoke(@NotNull Project project, @Nullable Editor editor, @NotNull RsBlockExpr element) {
        RsLabelDecl labelDecl = element.getLabelDecl();
        if (labelDecl == null) return;
        String labelName = labelDecl.getName();
        if (labelName == null) return;
        element.replace(new RsPsiFactory(project).createLoop(element.getBlock().getText(), labelName));
    }
}
