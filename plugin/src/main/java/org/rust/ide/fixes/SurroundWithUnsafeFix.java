/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.fixes;

import consulo.codeEditor.Editor;
import consulo.project.Project;
import consulo.language.psi.PsiElement;
import consulo.language.psi.util.PsiTreeUtil;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.rust.RsBundle;
import org.rust.lang.core.psi.RsExpr;
import org.rust.lang.core.psi.RsExprStmt;
import org.rust.lang.core.psi.RsPsiFactory;

public class SurroundWithUnsafeFix extends RsQuickFixBase<RsExpr> {

    public SurroundWithUnsafeFix(@Nonnull RsExpr expr) {
        super(expr);
    }

    @Nonnull
        public consulo.localize.LocalizeValue getFamilyName() {
        return getText();
    }

    @Nonnull
    @Override
    public consulo.localize.LocalizeValue getText() {
        return consulo.localize.LocalizeValue.of(RsBundle.message("intention.name.surround.with.unsafe.block"));
    }

    @Override
    public void invoke(@Nonnull Project project, @Nullable Editor editor, @Nonnull RsExpr element) {
        RsExprStmt exprStmt = PsiTreeUtil.getParentOfType(element, RsExprStmt.class, true);
        PsiElement target = exprStmt != null ? exprStmt : element;
        PsiElement unsafeBlockExpr = new RsPsiFactory(project).createUnsafeBlockExprOrStmt(target);
        target.replace(unsafeBlockExpr);
    }
}
