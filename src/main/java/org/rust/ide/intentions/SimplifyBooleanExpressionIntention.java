/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.intentions;

import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.rust.RsBundle;
import org.rust.ide.utils.BooleanExprSimplifier;
import org.rust.ide.utils.PsiModificationUtil;
import org.rust.lang.core.psi.RsExpr;
import org.rust.lang.core.psi.ext.PsiElementExt;

public class SimplifyBooleanExpressionIntention extends RsElementBaseIntentionAction<RsExpr> {
    @NotNull
    @Override
    public String getText() {
        return RsBundle.message("intention.name.simplify.boolean.expression");
    }

    @NotNull
    @Override
    public String getFamilyName() {
        return RsBundle.message("intention.family.name.simplify.boolean.expression");
    }

    @Nullable
    @Override
    public RsExpr findApplicableContext(@NotNull Project project, @NotNull Editor editor, @NotNull PsiElement element) {
        RsExpr expr = PsiElementExt.ancestorStrict(element, RsExpr.class);
        if (expr == null) return null;

        // Walk up ancestors while they are RsExpr
        RsExpr candidate = null;
        PsiElement current = expr;
        while (current instanceof RsExpr) {
            if (BooleanExprSimplifier.canBeSimplified((RsExpr) current)) {
                candidate = (RsExpr) current;
            }
            current = current.getParent();
        }
        if (candidate != null && PsiModificationUtil.canReplace(candidate)) {
            return candidate;
        }
        return null;
    }

    @Override
    public void invoke(@NotNull Project project, @NotNull Editor editor, @NotNull RsExpr ctx) {
        RsExpr simplified = new BooleanExprSimplifier(project).simplify(ctx);
        if (simplified == null) return;
        ctx.replace(simplified);
    }
}
