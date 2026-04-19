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
import org.rust.lang.core.psi.ext.UnaryOperator;
import org.rust.lang.core.psi.ext.RsUnaryExprUtil;

/**
 * Fix that converts the given immutable reference to a mutable reference.
 */
public class ChangeRefToMutableFix extends RsQuickFixBase<RsUnaryExpr> {

    public ChangeRefToMutableFix(@NotNull RsUnaryExpr expr) {
        super(expr);
    }

    @NotNull
    @Override
    public String getText() {
        return RsBundle.message("intention.name.change.reference.to.mutable");
    }

    @NotNull
    @Override
    public String getFamilyName() {
        return getText();
    }

    @Override
    public void invoke(@NotNull Project project, @Nullable Editor editor, @NotNull RsUnaryExpr element) {
        if (RsUnaryExprUtil.getOperatorType(element) != UnaryOperator.REF) return;
        RsExpr innerExpr = element.getExpr();
        if (innerExpr == null) return;

        RsExpr mutableExpr = new RsPsiFactory(project).tryCreateExpression("&mut " + innerExpr.getText());
        if (mutableExpr == null) return;
        element.replace(mutableExpr);
    }
}
