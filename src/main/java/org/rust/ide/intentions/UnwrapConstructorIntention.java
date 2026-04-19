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
import org.rust.ide.utils.PsiModificationUtil;
import org.rust.lang.core.psi.RsCallExpr;
import org.rust.lang.core.psi.RsExpr;
import org.rust.lang.core.psi.RsPathExpr;
import org.rust.lang.core.psi.ext.PsiElementExt;
import org.rust.lang.core.psi.ext.RsFieldsOwner;

import java.util.List;

public class UnwrapConstructorIntention extends RsElementBaseIntentionAction<UnwrapConstructorIntention.Context> {
    @NotNull
    @Override
    public String getFamilyName() {
        return RsBundle.message("intention.family.name.unwrap.enum.or.tuple.struct.constructor.from.expression");
    }

    public static class Context {
        private final RsCallExpr myCall;
        private final RsExpr myArgument;

        public Context(@NotNull RsCallExpr call, @NotNull RsExpr argument) {
            myCall = call;
            myArgument = argument;
        }

        @NotNull
        public RsCallExpr getCall() {
            return myCall;
        }

        @NotNull
        public RsExpr getArgument() {
            return myArgument;
        }
    }

    @Nullable
    @Override
    public Context findApplicableContext(@NotNull Project project, @NotNull Editor editor, @NotNull PsiElement element) {
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

        setText(RsBundle.message("intention.name.unwrap.from.expression", pathExpr.getText()));

        return new Context(call, argument);
    }

    @Override
    public void invoke(@NotNull Project project, @NotNull Editor editor, @NotNull Context ctx) {
        ctx.getCall().replace(ctx.getArgument());
    }
}
