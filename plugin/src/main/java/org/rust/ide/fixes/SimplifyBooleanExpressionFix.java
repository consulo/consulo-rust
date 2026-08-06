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
import org.rust.ide.utils.BooleanExprSimplifier;
import org.rust.ide.utils.PurityUtil;
import org.rust.lang.core.psi.RsExpr;
import org.rust.ide.utils.ExprUtil;

public class SimplifyBooleanExpressionFix extends RsQuickFixBase<RsExpr> {

    public SimplifyBooleanExpressionFix(@Nonnull RsExpr expr) {
        super(expr);
    }

    @Nonnull
    @Override
    public consulo.localize.LocalizeValue getText() {
        return consulo.localize.LocalizeValue.of(RsBundle.message("intention.name.simplify.boolean.expression"));
    }

    @Nonnull
        public consulo.localize.LocalizeValue getFamilyName() {
        return getText();
    }

    @Override
    public void invoke(@Nonnull Project project, @Nullable Editor editor, @Nonnull RsExpr element) {
        Boolean isPure = ExprUtil.isPure(element);
        if (Boolean.TRUE.equals(isPure) && BooleanExprSimplifier.canBeSimplified(element)) {
            RsExpr simplified = new BooleanExprSimplifier(project).simplify(element);
            if (simplified == null) return;
            element.replace(simplified);
        }
    }
}
