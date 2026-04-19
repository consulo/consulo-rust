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
import org.rust.ide.utils.PsiModificationUtil;
import org.rust.ide.utils.ExprUtil;
import org.rust.lang.core.psi.*;
import org.rust.lang.core.psi.ext.LogicOp;
import org.rust.lang.core.psi.ext.PsiElementExt;
import org.rust.lang.core.psi.ext.RsBinaryExprUtil;
import org.rust.lang.core.psi.ext.RsBinaryOpUtil;
import org.rust.lang.core.psi.ext.RsFunctionUtil;

public class SplitIfIntention extends RsElementBaseIntentionAction<SplitIfIntention.Context> {
    @NotNull
    @Override
    public String getText() {
        return RsBundle.message("intention.name.split.into.if.s");
    }

    @NotNull
    @Override
    public String getFamilyName() {
        return RsBundle.message("intention.family.name.split.if");
    }

    @NotNull
    @Override
    public InvokeInside getAttributeMacroHandlingStrategy() {
        return InvokeInside.MACRO_CALL;
    }

    public static class Context {
        private final RsBinaryOp myBinaryOp;
        private final LogicOp myOperatorType;
        private final RsExpr myConditionExpr;
        private final RsIfExpr myIfExpr;

        public Context(@NotNull RsBinaryOp binaryOp, @NotNull LogicOp operatorType,
                       @NotNull RsExpr conditionExpr, @NotNull RsIfExpr ifExpr) {
            myBinaryOp = binaryOp;
            myOperatorType = operatorType;
            myConditionExpr = conditionExpr;
            myIfExpr = ifExpr;
        }

        @NotNull
        public RsBinaryOp getBinaryOp() {
            return myBinaryOp;
        }

        @NotNull
        public LogicOp getOperatorType() {
            return myOperatorType;
        }

        @NotNull
        public RsExpr getConditionExpr() {
            return myConditionExpr;
        }

        @NotNull
        public RsIfExpr getIfExpr() {
            return myIfExpr;
        }
    }

    @Nullable
    @Override
    public Context findApplicableContext(@NotNull Project project, @NotNull Editor editor, @NotNull PsiElement element) {
        RsBinaryExpr binExpr = PsiElementExt.ancestorStrict(element, RsBinaryExpr.class);
        if (binExpr == null) return null;
        if (!(RsBinaryOpUtil.getOperatorType(binExpr) instanceof LogicOp)) return null;
        RsBinaryOp binaryOp = binExpr.getBinaryOp();
        if (!(RsBinaryOpUtil.getOperatorType(binaryOp) instanceof LogicOp)) return null;
        LogicOp operatorType = (LogicOp) RsBinaryOpUtil.getOperatorType(binaryOp);
        if (element.getParent() != binaryOp) return null;
        RsCondition condition = findCondition(binExpr);
        if (condition == null) return null;
        RsExpr condExpr = condition.getExpr();
        if (condExpr != null && PsiElementExt.descendantOfTypeOrSelf(condExpr, RsLetExpr.class) != null) return null;
        RsExpr conditionExpr = ExprUtil.skipParenExprDown(condition);
        if (conditionExpr == null) return null;
        RsIfExpr ifStatement = PsiElementExt.ancestorOrSelf(condition, RsIfExpr.class);
        if (ifStatement == null) return null;
        if (!PsiModificationUtil.canReplace(ifStatement)) return null;
        return new Context(binaryOp, operatorType, conditionExpr, ifStatement);
    }

    @Override
    public void invoke(@NotNull Project project, @NotNull Editor editor, @NotNull Context ctx) {
        RsBinaryOp binaryOp = ctx.getBinaryOp();
        LogicOp operatorType = ctx.getOperatorType();
        RsExpr conditionExpr = ctx.getConditionExpr();
        RsIfExpr ifStatement = ctx.getIfExpr();
        RsBlock block = ifStatement.getBlock();
        String thenBranch = block != null ? block.getText() : "{ }";
        RsElseBranch elseBranch = ifStatement.getElseBranch();
        String elseBranchText = elseBranch != null ? elseBranch.getText() : "";

        String leftCondition = leftPart(conditionExpr, binaryOp);
        String rightCondition = rightPart(conditionExpr, binaryOp);

        String generatedCode;
        if (operatorType == LogicOp.AND) {
            generatedCode = "if " + leftCondition + " { if " + rightCondition + " " + thenBranch + " " + elseBranchText + " } " + elseBranchText;
        } else {
            generatedCode = "if " + leftCondition + " " + thenBranch + " else if " + rightCondition + " " + thenBranch + " " + elseBranchText;
        }

        RsIfExpr newIfStatement = (RsIfExpr) new RsPsiFactory(project).createExpression(generatedCode);
        ifStatement.replace(newIfStatement);
    }

    @NotNull
    private static String leftPart(@NotNull RsExpr condition, @NotNull RsBinaryOp op) {
        return condition.getText().substring(0, op.getTextOffset() - condition.getTextOffset());
    }

    @NotNull
    private static String rightPart(@NotNull RsExpr condition, @NotNull RsBinaryOp op) {
        return condition.getText().substring(op.getTextOffset() + op.getTextLength() - condition.getTextOffset());
    }

    @Nullable
    private static RsCondition findCondition(@NotNull RsBinaryExpr binExpr) {
        PsiElement parent = ExprUtil.skipParenExprUp(binExpr).getParent();
        while (parent instanceof RsBinaryExpr && RsBinaryExprUtil.getOperatorType((RsBinaryExpr) parent) == RsBinaryExprUtil.getOperatorType(binExpr)) {
            parent = parent.getParent();
        }
        if (parent instanceof RsCondition) {
            return (RsCondition) parent;
        }
        return null;
    }
}
