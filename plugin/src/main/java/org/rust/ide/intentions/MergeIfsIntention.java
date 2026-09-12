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
import org.rust.lang.core.psi.*;
import org.rust.lang.core.psi.ext.LogicOp;
import org.rust.lang.core.psi.ext.PsiElementExt;
import org.rust.lang.core.psi.ext.RsBlockUtil;
import org.rust.lang.core.psi.ext.RsBinaryExprUtil;
import org.rust.lang.core.psi.ext.RsFunctionUtil;
import consulo.localize.LocalizeValue;

public class MergeIfsIntention extends RsElementBaseIntentionAction<MergeIfsIntention.Context> {
    @Nonnull
    @Override
    public consulo.localize.LocalizeValue getText() {
        return consulo.localize.LocalizeValue.of(RsBundle.message("intention.name.merge.with.nested.if.expression"));
        }

    @Nonnull
    public consulo.localize.LocalizeValue getFamilyName() {
        return getText();
        }

    @Nonnull
    @Override
    public InvokeInside getAttributeMacroHandlingStrategy() {
        return InvokeInside.MACRO_CALL;
    }

    public static class Context {
        private final RsBlock myIfExprBlock;
        @Nullable
        private final RsBlock myNestedIfExprBlock;
        private final RsExpr myIfCondition;
        private final RsExpr myNestedIfCondition;

        public Context(@Nonnull RsBlock ifExprBlock, @Nullable RsBlock nestedIfExprBlock,
                       @Nonnull RsExpr ifCondition, @Nonnull RsExpr nestedIfCondition) {
            myIfExprBlock = ifExprBlock;
            myNestedIfExprBlock = nestedIfExprBlock;
            myIfCondition = ifCondition;
            myNestedIfCondition = nestedIfCondition;
        }

        @Nonnull
        public RsBlock getIfExprBlock() {
            return myIfExprBlock;
        }

        @Nullable
        public RsBlock getNestedIfExprBlock() {
            return myNestedIfExprBlock;
        }

        @Nonnull
        public RsExpr getIfCondition() {
            return myIfCondition;
        }

        @Nonnull
        public RsExpr getNestedIfCondition() {
            return myNestedIfCondition;
        }
    }

    @Nullable
    @Override
    public Context findApplicableContext(@Nonnull Project project, @Nonnull Editor editor, @Nonnull PsiElement element) {
        RsIfExpr ifExpr = PsiElementExt.ancestorStrict(element, RsIfExpr.class);
        if (ifExpr == null) return null;
        if (element != ifExpr.getIf()) return null;

        RsBlock ifExprBlock = ifExpr.getBlock();
        if (ifExprBlock == null) return null;
        RsExprStmt singleTailStmt = RsBlockUtil.singleTailStmt(ifExprBlock);
        if (singleTailStmt == null) return null;
        RsExpr tailExpr = singleTailStmt.getExpr();
        if (!(tailExpr instanceof RsIfExpr)) return null;
        RsIfExpr nestedIfExpr = (RsIfExpr) tailExpr;

        RsCondition ifConditionElement = ifExpr.getCondition();
        if (ifConditionElement == null) return null;
        RsExpr ifCondition = ifConditionElement.getExpr();
        if (ifCondition == null) return null;
        RsCondition nestedIfCondition = nestedIfExpr.getCondition();
        if (nestedIfCondition == null) return null;
        RsBlock nestedIfExprBlock = nestedIfExpr.getBlock();

        if (PsiElementExt.descendantOfTypeOrSelf(ifCondition, RsLetExpr.class) != null) return null;
        RsExpr nestedCondExpr = nestedIfCondition.getExpr();
        if (nestedCondExpr == null) return null;
        if (PsiElementExt.descendantOfTypeOrSelf(nestedCondExpr, RsLetExpr.class) != null) return null;
        if (ifExpr.getElseBranch() != null || nestedIfExpr.getElseBranch() != null) return null;
        if (!PsiModificationUtil.canReplace(ifCondition)) return null;
        if (nestedIfExprBlock != null && !PsiModificationUtil.canReplace(ifExprBlock)) return null;

        return new Context(ifExprBlock, nestedIfExprBlock, ifCondition, nestedCondExpr);
    }

    @Override
    public void invoke(@Nonnull Project project, @Nonnull Editor editor, @Nonnull Context ctx) {
        RsExpr condition = createConjunction(ctx.getIfCondition(), ctx.getNestedIfCondition());
        ctx.getIfCondition().replace(condition);
        RsBlock nestedBlock = ctx.getNestedIfExprBlock();
        if (nestedBlock != null) {
            ctx.getIfExprBlock().replace(nestedBlock);
        }
    }

    @Nonnull
    private RsExpr createConjunction(@Nonnull RsExpr expr1, @Nonnull RsExpr expr2) {
        String text1 = getTextForConjunctionOperand(expr1);
        String text2 = getTextForConjunctionOperand(expr2);
        return new RsPsiFactory(expr1.getProject()).createExpression(text1 + " && " + text2);
    }

    @Nonnull
    private String getTextForConjunctionOperand(@Nonnull RsExpr expr) {
        if (expr instanceof RsBinaryExpr && RsBinaryExprUtil.getOperatorType((RsBinaryExpr) expr) == LogicOp.OR) {
            return "(" + expr.getText() + ")";
        }
        return expr.getText();
    }
}
