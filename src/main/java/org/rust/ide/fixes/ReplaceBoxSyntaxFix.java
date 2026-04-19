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
import org.rust.lang.core.psi.RsExpr;
import org.rust.lang.core.psi.RsPsiFactory;
import org.rust.lang.core.psi.RsUnaryExpr;

public class ReplaceBoxSyntaxFix extends RsQuickFixBase<RsUnaryExpr> {

    public ReplaceBoxSyntaxFix(@NotNull RsUnaryExpr element) {
        super(element);
    }

    @NotNull
    @Override
    public String getFamilyName() {
        return RsBundle.message("intention.family.name.replace.box.with.box.new");
    }

    @NotNull
    @Override
    public String getText() {
        return getFamilyName();
    }

    @Override
    public void invoke(@NotNull Project project, @Nullable Editor editor, @NotNull RsUnaryExpr element) {
        if (element.getBox() == null) return;
        RsExpr expr = element.getExpr();
        if (expr == null) return;
        element.replace(new RsPsiFactory(project).createBox(expr.getText()));
    }
}
