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
import org.rust.lang.core.psi.*;
import org.rust.lang.core.psi.ext.RsMacroCallUtil;
import org.rust.lang.core.psi.ext.RsExprUtil;

public class ChangeTryMacroToTryOperator extends RsQuickFixBase<RsMacroCall> {

    public ChangeTryMacroToTryOperator(@NotNull RsMacroCall element) {
        super(element);
    }

    @NotNull
    @Override
    public String getText() {
        return RsBundle.message("intention.name.change.try.to");
    }

    @NotNull
    @Override
    public String getFamilyName() {
        return getName();
    }

    @Override
    public void invoke(@NotNull Project project, @Nullable Editor editor, @NotNull RsMacroCall element) {
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
