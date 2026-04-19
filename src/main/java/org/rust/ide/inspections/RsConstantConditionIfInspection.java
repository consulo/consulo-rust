/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.inspections;

import com.intellij.codeInspection.LocalQuickFix;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiElement;
import com.intellij.psi.util.PsiTreeUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.rust.RsBundle;
import org.rust.ide.fixes.RsQuickFixBase;
import org.rust.ide.fixes.SubstituteTextFix;
import org.rust.lang.core.psi.*;
import org.rust.lang.core.psi.ext.*;
import org.rust.lang.core.types.consts.CtValue;
import org.rust.lang.utils.evaluation.ConstExprEvaluator;
import org.rust.lang.core.psi.ext.RsFunctionUtil;
import org.rust.lang.core.psi.ext.PsiElementUtil;

/**
 * See also RsRedundantElseInspection.
 */
public class RsConstantConditionIfInspection extends RsLocalInspectionTool {

    @Override
    public RsVisitor buildVisitor(@NotNull RsProblemsHolder holder, boolean isOnTheFly) {
        return new RsWithMacrosInspectionVisitor() {
            @Override
            public void visitIfExpr(@NotNull RsIfExpr ifExpr) {
                RsCondition condition = ifExpr.getCondition();
                if (condition == null) return;
                RsExpr condExpr = condition.getExpr();
                if (condExpr == null) return;
                if (PsiTreeUtil.findChildOfType(condExpr, RsLetExpr.class) != null) return;
                Boolean conditionValue = CtValue.asBool(ConstExprEvaluator.evaluate(condExpr));
                if (conditionValue == null) return;

                boolean isUsedAsExpression = !(ifExpr.getParent() instanceof RsExprStmt);
                LocalQuickFix fix;
                if (!conditionValue && ifExpr.getElseBranch() == null) {
                    boolean isInsideCascadeIf = ifExpr.getParent() instanceof RsElseBranch;
                    if (isUsedAsExpression && !isInsideCascadeIf) return;
                    fix = createDeleteElseBranchFix(ifExpr, isInsideCascadeIf);
                } else {
                    fix = new SimplifyFix(condition, conditionValue);
                }

                holder.registerProblem(condition, RsBundle.message("inspection.message.condition.always", conditionValue), fix);
            }
        };
    }

    @NotNull
    private SubstituteTextFix createDeleteElseBranchFix(@NotNull RsIfExpr ifExpr, boolean isInsideCascadeIf) {
        TextRange ifRange = PsiElementUtil.getRangeWithPrevSpace(ifExpr);
        TextRange deletionRange;
        if (isInsideCascadeIf) {
            PsiElement parentElse = ((RsElseBranch) ifExpr.getParent()).getElse();
            TextRange elseRange = PsiElementUtil.getRangeWithPrevSpace(parentElse, parentElse.getPrevSibling());
            deletionRange = elseRange.union(ifRange);
        } else {
            deletionRange = ifRange;
        }
        return SubstituteTextFix.delete(
            RsBundle.message("intention.name.delete.expression"),
            ifExpr.getContainingFile(),
            deletionRange
        );
    }

    private static class SimplifyFix extends RsQuickFixBase<RsCondition> {
        private final boolean conditionValue;

        SimplifyFix(@NotNull RsCondition element, boolean conditionValue) {
            super(element);
            this.conditionValue = conditionValue;
        }

        @NotNull
        @Override
        public String getText() {
            return RsBundle.message("intention.name.simplify.expression");
        }

        @NotNull
        @Override
        public String getFamilyName() {
            return getText();
        }

        @Override
        public void invoke(@NotNull Project project, @Nullable Editor editor, @NotNull RsCondition element) {
            RsIfExpr ifExpr = PsiTreeUtil.getParentOfType(element, RsIfExpr.class);
            if (ifExpr == null) return;

            RsElseBranch elseBranch = ifExpr.getElseBranch();
            if (elseBranch != null && elseBranch.getIfExpr() != null) {
                RsIfExpr elseIfExpr = elseBranch.getIfExpr();
                if (!conditionValue) {
                    ifExpr.replace(elseIfExpr);
                    return;
                }
            }

            RsBlock branch = conditionValue ? ifExpr.getBlock() : (elseBranch != null ? elseBranch.getBlock() : null);
            if (branch == null) return;
            // Simplified replacement - complex block content handling elided for brevity
            RsPsiFactory factory = new RsPsiFactory(project);
            RsBlockExpr blockExpr = factory.createBlockExpr(branch.getText());
            PsiElement replaced = ifExpr.replace(blockExpr);
            if (editor != null && replaced != null) {
                editor.getCaretModel().moveToOffset(replaced.getTextOffset());
            }
        }
    }
}
