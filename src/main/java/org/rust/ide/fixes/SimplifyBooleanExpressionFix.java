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
import org.rust.ide.utils.BooleanExprSimplifier;
import org.rust.ide.utils.PurityUtil;
import org.rust.lang.core.psi.RsExpr;
import org.rust.ide.utils.ExprUtil;

public class SimplifyBooleanExpressionFix extends RsQuickFixBase<RsExpr> {

    public SimplifyBooleanExpressionFix(@NotNull RsExpr expr) {
        super(expr);
    }

    @NotNull
    @Override
    public String getText() {
        return RsBundle.message("intention.name.simplify.boolean.expression");
    }

    @NotNull
    @Override
    public String getFamilyName() {
        return getText();
    }

    @Override
    public void invoke(@NotNull Project project, @Nullable Editor editor, @NotNull RsExpr element) {
        Boolean isPure = ExprUtil.isPure(element);
        if (Boolean.TRUE.equals(isPure) && BooleanExprSimplifier.canBeSimplified(element)) {
            RsExpr simplified = new BooleanExprSimplifier(project).simplify(element);
            if (simplified == null) return;
            element.replace(simplified);
        }
    }
}
