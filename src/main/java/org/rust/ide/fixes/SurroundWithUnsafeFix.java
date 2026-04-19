/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.fixes;

import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElement;
import com.intellij.psi.util.PsiTreeUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.rust.RsBundle;
import org.rust.lang.core.psi.RsExpr;
import org.rust.lang.core.psi.RsExprStmt;
import org.rust.lang.core.psi.RsPsiFactory;

public class SurroundWithUnsafeFix extends RsQuickFixBase<RsExpr> {

    public SurroundWithUnsafeFix(@NotNull RsExpr expr) {
        super(expr);
    }

    @NotNull
    @Override
    public String getFamilyName() {
        return getText();
    }

    @NotNull
    @Override
    public String getText() {
        return RsBundle.message("intention.name.surround.with.unsafe.block");
    }

    @Override
    public void invoke(@NotNull Project project, @Nullable Editor editor, @NotNull RsExpr element) {
        RsExprStmt exprStmt = PsiTreeUtil.getParentOfType(element, RsExprStmt.class, true);
        PsiElement target = exprStmt != null ? exprStmt : element;
        PsiElement unsafeBlockExpr = new RsPsiFactory(project).createUnsafeBlockExprOrStmt(target);
        target.replace(unsafeBlockExpr);
    }
}
