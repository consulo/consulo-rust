/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.inspections.lints;

import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.rust.RsBundle;
import org.rust.ide.fixes.RsQuickFixBase;
import org.rust.ide.inspections.RsProblemsHolder;
import org.rust.ide.inspections.RsWithMacrosInspectionVisitor;
import org.rust.lang.core.psi.ext.RsExprUtil;
import org.rust.lang.core.psi.*;
import org.rust.lang.core.psi.ext.RsPsiElementUtil;

import java.util.Collections;
import org.rust.lang.core.psi.ext.RsFunctionUtil;
import org.rust.lang.core.psi.ext.RsPsiElementExtUtil;

/**
 * Change `while true` to `loop`.
 */
public class RsWhileTrueLoopInspection extends RsLintInspection {

    @NotNull
    @Override
    public String getDisplayName() {
        return RsBundle.message("while.true.loop");
    }

    @NotNull
    @Override
    protected RsLint getLint(@NotNull PsiElement element) {
        return RsLint.WhileTrue;
    }

    @NotNull
    @Override
    public RsVisitor buildVisitor(@NotNull RsProblemsHolder holder, boolean isOnTheFly) {
        return new RsWithMacrosInspectionVisitor() {
            @Override
            public void visitWhileExpr(@NotNull RsWhileExpr o) {
                RsCondition condition = o.getCondition();
                if (condition == null) return;
                RsExpr condExprRaw = condition.getExpr();
                if (condExprRaw == null) return;
                RsExpr condExpr = RsExprUtil.unwrapParenExprs(condExprRaw);
                if (!(condExpr instanceof RsLitExpr)) return;
                if (o.getBlock() == null) return;
                if (condExpr.textMatches("true")) {
                    registerLintProblem(
                        holder,
                        o,
                        RsBundle.message("inspection.message.denote.infinite.loops.with.loop"),
                        TextRange.create(
                            o.getWhile().getStartOffsetInParent(),
                            RsPsiElementExtUtil.getEndOffsetInParent(condition)
                        ),
                        RsLintHighlightingType.WEAK_WARNING,
                        Collections.singletonList(new UseLoopFix(o))
                    );
                }
            }
        };
    }

    @Override
    public boolean isSyntaxOnly() {
        return true;
    }

    private static class UseLoopFix extends RsQuickFixBase<RsWhileExpr> {

        UseLoopFix(@NotNull RsWhileExpr element) {
            super(element);
        }

        @NotNull
        @Override
        public String getFamilyName() {
            return RsBundle.message("intention.family.name.use.loop");
        }

        @NotNull
        @Override
        public String getText() {
            return getFamilyName();
        }

        @Override
        public void invoke(@NotNull Project project, @Nullable Editor editor, @NotNull RsWhileExpr element) {
            RsBlock block = element.getBlock();
            if (block == null) return;
            RsLabelDecl labelDecl = element.getLabelDecl();
            String label = labelDecl != null ? labelDecl.getText() : "";
            RsLoopExpr loopExpr = (RsLoopExpr) new RsPsiFactory(project).createExpression(label + "loop " + block.getText());
            element.replace(loopExpr);
        }
    }
}
