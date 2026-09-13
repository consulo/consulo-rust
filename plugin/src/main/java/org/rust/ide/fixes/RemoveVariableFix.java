/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.fixes;

import consulo.codeEditor.Editor;
import consulo.project.Project;
import consulo.language.psi.util.PsiTreeUtil;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.rust.RsBundle;
import org.rust.lang.core.psi.*;
import org.rust.lang.core.psi.ext.impl.RsExprUtil;
import org.rust.lang.core.psi.ext.impl.RsPatBindingUtil;
import consulo.localize.LocalizeValue;
import org.rust.lang.core.psi.impl.*;

/**
 * Fix that removes a variable.
 * A heuristic is used whether to also remove its expression or not.
 */
public class RemoveVariableFix extends RsQuickFixBase<RsPatBinding> {

    private final String bindingName;

    public RemoveVariableFix(@Nonnull RsPatBinding binding, @Nonnull String bindingName) {
        super(binding);
        this.bindingName = bindingName;
    }

    @Nonnull
    @Override
    public consulo.localize.LocalizeValue getText() {
        return consulo.localize.LocalizeValue.of(RsBundle.message("intention.name.remove.variable", bindingName));
    }

    @Nonnull
        public consulo.localize.LocalizeValue getFamilyName() {
        return consulo.localize.LocalizeValue.of(RsBundle.message("intention.family.name.remove.variable"));
    }

    @Override
    public void invoke(@Nonnull Project project, @Nullable Editor editor, @Nonnull RsPatBinding element) {
        RsPat topLevelPat = RsPatBindingUtil.getTopLevelPattern(element);
        if (!(topLevelPat instanceof RsPatIdent)) return;
        deleteVariable((RsPatIdent) topLevelPat);
    }

    private static void deleteVariable(@Nonnull RsPatIdent pat) {
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
