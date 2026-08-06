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
import org.rust.ide.utils.PsiModificationUtil;
import org.rust.lang.core.psi.RsCallExpr;
import org.rust.lang.core.psi.RsExpr;
import org.rust.lang.core.psi.RsPathExpr;
import org.rust.lang.core.psi.ext.PsiElementExt;
import org.rust.lang.core.psi.ext.RsFieldsOwner;

import java.util.List;

public class UnwrapConstructorIntention extends RsElementBaseIntentionAction<UnwrapConstructorIntention.Context> {
    @Nonnull
    public consulo.localize.LocalizeValue getFamilyName() {
        return consulo.localize.LocalizeValue.of(RsBundle.message("intention.family.name.unwrap.enum.or.tuple.struct.constructor.from.expression"));
        }

    public static class Context {
        private final RsCallExpr myCall;
        private final RsExpr myArgument;

        public Context(@Nonnull RsCallExpr call, @Nonnull RsExpr argument) {
            myCall = call;
            myArgument = argument;
        }

        @Nonnull
        public RsCallExpr getCall() {
            return myCall;
        }

        @Nonnull
        public RsExpr getArgument() {
            return myArgument;
        }
    }

    @Nullable
    @Override
    public Context findApplicableContext(@Nonnull Project project, @Nonnull Editor editor, @Nonnull PsiElement element) {
        RsCallExpr call = PsiElementExt.ancestorStrict(element, RsCallExpr.class);
        if (call == null) return null;
        List<RsExpr> exprList = call.getValueArgumentList().getExprList();
        if (exprList.size() != 1) return null;
        RsExpr argument = exprList.get(0);

        RsExpr expr = call.getExpr();
        if (!(expr instanceof RsPathExpr)) return null;
        RsPathExpr pathExpr = (RsPathExpr) expr;
        PsiElement resolved = pathExpr.getPath().getReference() != null ? pathExpr.getPath().getReference().resolve() : null;
        if (!(resolved instanceof RsFieldsOwner)) return null;

        if (!PsiModificationUtil.canReplace(call)) return null;

        setText(consulo.localize.LocalizeValue.of(RsBundle.message("intention.name.unwrap.from.expression", pathExpr.getText())));

        return new Context(call, argument);
    }

    @Override
    public void invoke(@Nonnull Project project, @Nonnull Editor editor, @Nonnull Context ctx) {
        ctx.getCall().replace(ctx.getArgument());
    }
}
