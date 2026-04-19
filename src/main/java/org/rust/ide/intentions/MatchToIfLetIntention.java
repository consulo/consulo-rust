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
import org.rust.ide.intentions.util.macros.InvokeInside;
import org.rust.ide.refactoring.RsRefactoringUtilUtil;
import org.rust.ide.utils.PsiModificationUtil;
import org.rust.lang.core.psi.*;
import org.rust.lang.core.psi.ext.PsiElementExt;
import org.rust.lang.core.types.ty.TyUnit;
import org.rust.lang.core.types.infer.TypeInference;

import java.util.List;
import org.rust.lang.core.types.RsTypesUtil;
import org.rust.lang.core.psi.ext.RsFunctionUtil;

public class MatchToIfLetIntention extends RsElementBaseIntentionAction<MatchToIfLetIntention.Context> {
    @NotNull
    @Override
    public String getText() {
        return RsBundle.message("intention.name.convert.match.statement.to.if.let");
    }

    @NotNull
    @Override
    public String getFamilyName() {
        return getText();
    }

    @NotNull
    @Override
    public InvokeInside getAttributeMacroHandlingStrategy() {
        return InvokeInside.MACRO_CALL;
    }

    public static class Context {
        private final RsMatchExpr myMatch;
        private final RsExpr myMatchTarget;
        private final RsMatchBody myMatchBody;

        public Context(@NotNull RsMatchExpr match, @NotNull RsExpr matchTarget, @NotNull RsMatchBody matchBody) {
            myMatch = match;
            myMatchTarget = matchTarget;
            myMatchBody = matchBody;
        }

        @NotNull
        public RsMatchExpr getMatch() {
            return myMatch;
        }

        @NotNull
        public RsExpr getMatchTarget() {
            return myMatchTarget;
        }

        @NotNull
        public RsMatchBody getMatchBody() {
            return myMatchBody;
        }
    }

    @Nullable
    @Override
    public Context findApplicableContext(@NotNull Project project, @NotNull Editor editor, @NotNull PsiElement element) {
        RsMatchExpr matchExpr = PsiElementExt.ancestorStrict(element, RsMatchExpr.class);
        if (matchExpr == null) return null;
        if (element != matchExpr.getMatch()) return null;
        RsExpr matchTarget = matchExpr.getExpr();
        if (matchTarget == null) return null;
        RsMatchBody matchBody = matchExpr.getMatchBody();
        if (matchBody == null) return null;
        if (matchBody.getMatchArmList().isEmpty()) return null;
        for (RsMatchArm arm : matchBody.getMatchArmList()) {
            if (arm.getMatchArmGuard() != null || !arm.getOuterAttrList().isEmpty()) return null;
        }
        if (!PsiModificationUtil.canReplace(matchExpr)) return null;
        return new Context(matchExpr, matchTarget, matchBody);
    }

    @Override
    public void invoke(@NotNull Project project, @NotNull Editor editor, @NotNull Context ctx) {
        List<RsMatchArm> arms = ctx.getMatchBody().getMatchArmList();
        RsMatchArm lastArm = arms.isEmpty() ? null : arms.get(arms.size() - 1);
        if (lastArm == null) return;

        boolean hasUnitType = RsTypesUtil.getType(ctx.getMatch()) instanceof TyUnit;
        boolean lastArmHasBinding = org.rust.ide.refactoring.ExtraxtExpressionUtils.findBinding(lastArm.getPat()) != null;

        boolean lastArmExprEmpty = isExprEmpty(lastArm.getExpr());

        StringBuilder text = new StringBuilder();
        boolean hasElseBlock = false;

        for (int index = 0; index < arms.size(); index++) {
            RsMatchArm arm = arms.get(index);
            RsExpr expr = arm.getExpr();
            if (expr == null) continue;

            String armText = "if let " + arm.getPat().getText() + " = " + ctx.getMatchTarget().getText();
            if (index == 0) {
                // First, nothing special
            } else if (index == arms.size() - 1) {
                if (hasUnitType && lastArmExprEmpty) {
                    break;
                } else if (!lastArmHasBinding) {
                    armText = " else";
                    hasElseBlock = true;
                } else {
                    armText = " else " + armText;
                }
            } else {
                armText = " else " + armText;
            }
            text.append(armText);
            text.append(' ');

            String innerExprText;
            if (!hasUnitType && isExprEmpty(expr)) {
                innerExprText = "unreachable!()";
            } else if (expr instanceof RsBlockExpr) {
                RsBlock block = ((RsBlockExpr) expr).getBlock();
                String blockText = block.getText();
                int start = block.getLbrace().getStartOffsetInParent() + 1;
                PsiElement rbrace = block.getRbrace();
                int end = rbrace != null ? rbrace.getStartOffsetInParent() : blockText.length();
                innerExprText = blockText.substring(start, end);
            } else {
                innerExprText = expr.getText();
            }

            String exprText = "{\n    " + innerExprText + "\n}";
            text.append(exprText);
        }

        if (!hasUnitType && !hasElseBlock) {
            text.append(" else {\n    unreachable!()\n}");
        }

        RsPsiFactory factory = new RsPsiFactory(project);
        RsExpr ifLetExpr = factory.createExpression(text.toString());
        ctx.getMatch().replace(ifLetExpr);
    }

    private static boolean isExprEmpty(@Nullable RsExpr expr) {
        if (expr == null) return false;
        if (expr instanceof RsUnitExpr) return true;
        if (expr instanceof RsBlockExpr && ((RsBlockExpr) expr).getBlock().getChildren().length == 0) return true;
        return false;
    }
}
