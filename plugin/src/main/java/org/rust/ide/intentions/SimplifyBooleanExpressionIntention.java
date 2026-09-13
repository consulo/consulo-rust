/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.intentions;

import consulo.codeEditor.Editor;
import consulo.project.Project;
import consulo.language.psi.PsiElement;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.rust.RsBundle;
import org.rust.ide.utils.BooleanExprSimplifier;
import org.rust.ide.utils.PsiModificationUtil;
import org.rust.lang.core.psi.RsExpr;
import org.rust.lang.core.psi.ext.impl.PsiElementExt;
import consulo.localize.LocalizeValue;

public class SimplifyBooleanExpressionIntention extends RsElementBaseIntentionAction<RsExpr> {
    @Nonnull
    @Override
    public consulo.localize.LocalizeValue getText() {
        return consulo.localize.LocalizeValue.of(RsBundle.message("intention.name.simplify.boolean.expression"));
        }

    @Nonnull
    public consulo.localize.LocalizeValue getFamilyName() {
        return consulo.localize.LocalizeValue.of(RsBundle.message("intention.family.name.simplify.boolean.expression"));
        }

    @Nullable
    @Override
    public RsExpr findApplicableContext(@Nonnull Project project, @Nonnull Editor editor, @Nonnull PsiElement element) {
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
    public void invoke(@Nonnull Project project, @Nonnull Editor editor, @Nonnull RsExpr ctx) {
        RsExpr simplified = new BooleanExprSimplifier(project).simplify(ctx);
        if (simplified == null) return;
        ctx.replace(simplified);
    }
}
