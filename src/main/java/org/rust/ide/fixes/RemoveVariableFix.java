/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.fixes;

import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.psi.util.PsiTreeUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.rust.RsBundle;
import org.rust.lang.core.psi.*;
import org.rust.lang.core.psi.ext.RsExprUtil;
import org.rust.lang.core.psi.ext.RsPatBindingUtil;

/**
 * Fix that removes a variable.
 * A heuristic is used whether to also remove its expression or not.
 */
public class RemoveVariableFix extends RsQuickFixBase<RsPatBinding> {

    private final String bindingName;

    public RemoveVariableFix(@NotNull RsPatBinding binding, @NotNull String bindingName) {
        super(binding);
        this.bindingName = bindingName;
    }

    @NotNull
    @Override
    public String getText() {
        return RsBundle.message("intention.name.remove.variable", bindingName);
    }

    @NotNull
    @Override
    public String getFamilyName() {
        return RsBundle.message("intention.family.name.remove.variable");
    }

    @Override
    public void invoke(@NotNull Project project, @Nullable Editor editor, @NotNull RsPatBinding element) {
        RsPat topLevelPat = RsPatBindingUtil.getTopLevelPattern(element);
        if (!(topLevelPat instanceof RsPatIdent)) return;
        deleteVariable((RsPatIdent) topLevelPat);
    }

    private static void deleteVariable(@NotNull RsPatIdent pat) {
        RsLetDecl decl = PsiTreeUtil.getParentOfType(pat, RsLetDecl.class);
        if (decl == null) return;
        RsExpr expr = decl.getExpr();

        if (expr != null && RsExprUtil.getHasSideEffects(expr)) {
            RsPsiFactory factory = new RsPsiFactory(expr.getProject());
            if (decl.getSemicolon() != null) {
                RsExprStmt newExpr = factory.tryCreateExprStmtWithSemicolon(expr.getText());
                if (newExpr != null) {
                    decl.replace(newExpr);
                } else {
                    decl.replace(expr);
                }
            } else {
                decl.replace(expr);
            }
        } else {
            decl.delete();
        }
    }
}
