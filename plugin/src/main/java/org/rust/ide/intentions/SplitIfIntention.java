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
    @Nonnull
    @Override
    public consulo.localize.LocalizeValue getText() {
        return consulo.localize.LocalizeValue.of(RsBundle.message("intention.name.split.into.if.s"));
        }

    @Nonnull
    public consulo.localize.LocalizeValue getFamilyName() {
        return consulo.localize.LocalizeValue.of(RsBundle.message("intention.family.name.split.if"));
        }

    @Nonnull
    @Override
    public InvokeInside getAttributeMacroHandlingStrategy() {
        return InvokeInside.MACRO_CALL;
    }

    public static class Context {
        private final RsBinaryOp myBinaryOp;
        private final LogicOp myOperatorType;
        private final RsExpr myConditionExpr;
        private final RsIfExpr myIfExpr;

        public Context(@Nonnull RsBinaryOp binaryOp, @Nonnull LogicOp operatorType,
                       @Nonnull RsExpr conditionExpr, @Nonnull RsIfExpr ifExpr) {
            myBinaryOp = binaryOp;
            myOperatorType = operatorType;
            myConditionExpr = conditionExpr;
            myIfExpr = ifExpr;
        }

        @Nonnull
        public RsBinaryOp getBinaryOp() {
            return myBinaryOp;
        }

        @Nonnull
        public LogicOp getOperatorType() {
            return myOperatorType;
        }

        @Nonnull
        public RsExpr getConditionExpr() {
            return myConditionExpr;
        }

        @Nonnull
        public RsIfExpr getIfExpr() {
            return myIfExpr;
        }
    }

    @Nullable
    @Override
    public Context findApplicableContext(@Nonnull Project project, @Nonnull Editor editor, @Nonnull PsiElement element) {
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
    public void invoke(@Nonnull Project project, @Nonnull Editor editor, @Nonnull Context ctx) {
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

    @Nonnull
    private static String leftPart(@Nonnull RsExpr condition, @Nonnull RsBinaryOp op) {
        return condition.getText().substring(0, op.getTextOffset() - condition.getTextOffset());
    }

    @Nonnull
    private static String rightPart(@Nonnull RsExpr condition, @Nonnull RsBinaryOp op) {
        return condition.getText().substring(op.getTextOffset() + op.getTextLength() - condition.getTextOffset());
    }

    @Nullable
    private static RsCondition findCondition(@Nonnull RsBinaryExpr binExpr) {
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
