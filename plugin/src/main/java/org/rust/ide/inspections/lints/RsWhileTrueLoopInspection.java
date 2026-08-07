/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.inspections.lints;

import consulo.codeEditor.Editor;
import consulo.project.Project;
import consulo.document.util.TextRange;
import consulo.language.psi.PsiElement;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
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
import consulo.localize.LocalizeValue;
import consulo.language.editor.rawHighlight.HighlightDisplayLevel;
import consulo.annotation.component.ExtensionImpl;

/**
 * Change `while true` to `loop`.
 */
@ExtensionImpl
public class RsWhileTrueLoopInspection extends RsLintInspection {

    @Nonnull
    @Override
    protected RsLint getLint(@Nonnull PsiElement element) {
        return RsLint.WhileTrue;
    }

    @Nonnull
    @Override
    public RsVisitor buildVisitor(@Nonnull RsProblemsHolder holder, boolean isOnTheFly) {
        return new RsWithMacrosInspectionVisitor() {
            @Override
            public void visitWhileExpr(@Nonnull RsWhileExpr o) {
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

        UseLoopFix(@Nonnull RsWhileExpr element) {
            super(element);
        }

        @Nonnull
        @Override
        public consulo.localize.LocalizeValue getFamilyName() {
            return consulo.localize.LocalizeValue.of(RsBundle.message("intention.family.name.use.loop"));
            }

        @Nonnull
        @Override
        public consulo.localize.LocalizeValue getText() {
            return getFamilyName();
            }

        @Override
        public void invoke(@Nonnull Project project, @Nullable Editor editor, @Nonnull RsWhileExpr element) {
            RsBlock block = element.getBlock();
            if (block == null) return;
            RsLabelDecl labelDecl = element.getLabelDecl();
            String label = labelDecl != null ? labelDecl.getText() : "";
            RsLoopExpr loopExpr = (RsLoopExpr) new RsPsiFactory(project).createExpression(label + "loop " + block.getText());
            element.replace(loopExpr);
        }
    }

    @Override
    @jakarta.annotation.Nonnull
    public consulo.localize.LocalizeValue getDisplayName() {
        return consulo.localize.LocalizeValue.of(org.rust.RsBundle.message("inspection.rs.while.true.loop.display.name"));
    }

    @Override
    @jakarta.annotation.Nonnull
    public consulo.localize.LocalizeValue getGroupDisplayName() {
        return consulo.localize.LocalizeValue.of(org.rust.RsBundle.message("lints"));
    }

    @Nonnull
    @Override
    public LocalizeValue[] getGroupPath() {
        return new LocalizeValue[]{LocalizeValue.of(RsBundle.message("rust"))};
    }

    @Nonnull
    @Override
    public HighlightDisplayLevel getDefaultLevel() {
        return HighlightDisplayLevel.WEAK_WARNING;
    }
}
