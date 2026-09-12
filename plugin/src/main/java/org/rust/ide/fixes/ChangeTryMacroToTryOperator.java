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
import org.rust.lang.core.psi.*;
import org.rust.lang.core.psi.ext.RsMacroCallUtil;
import org.rust.lang.core.psi.ext.RsExprUtil;
import consulo.localize.LocalizeValue;

public class ChangeTryMacroToTryOperator extends RsQuickFixBase<RsMacroCall> {

    public ChangeTryMacroToTryOperator(@Nonnull RsMacroCall element) {
        super(element);
    }

    @Nonnull
    @Override
    public consulo.localize.LocalizeValue getText() {
        return consulo.localize.LocalizeValue.of(RsBundle.message("intention.name.change.try.to"));
    }

    @Nonnull
        public consulo.localize.LocalizeValue getFamilyName() {
        return getName();
    }

    @Override
    public void invoke(@Nonnull Project project, @Nullable Editor editor, @Nonnull RsMacroCall element) {
        RsPsiFactory factory = new RsPsiFactory(project);
        String body = RsMacroCallUtil.getMacroBody(element);
        if (body == null) return;
        RsExpr expr = factory.tryCreateExpression(body);
        if (expr == null) return;
        RsTryExpr tryExpr = (RsTryExpr) factory.createExpression("()?");
        tryExpr.getExpr().replace(expr);
        RsExprUtil.replaceWithExpr(element, tryExpr);
    }
}
