/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.inspections;

import org.rust.lang.core.psi.ext.RsElementUtil;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.rust.RsBundle;
import org.rust.ide.fixes.RsQuickFixBase;
import org.rust.lang.core.psi.*;
import org.rust.lang.core.psi.ext.RsElement;
import org.rust.lang.core.psi.ext.RsMatchExprUtil;
import org.rust.lang.core.psi.ext.RsStmtUtil;
import org.rust.openapiext.Testmark;

import java.util.ArrayList;
import java.util.List;
import org.rust.lang.core.psi.ext.RsFunctionUtil;

public class RsLiftInspection extends RsLocalInspectionTool {

    @Override
    public RsVisitor buildVisitor(@NotNull RsProblemsHolder holder, boolean isOnTheFly) {
        return new RsWithMacrosInspectionVisitor() {
            @Override
            public void visitIfExpr(@NotNull RsIfExpr o) {
                if (o.getParent() instanceof RsElseBranch) return;
                checkExpr(o, o.getIf());
            }

            @Override
            public void visitMatchExpr(@NotNull RsMatchExpr o) {
                checkExpr(o, o.getMatch());
            }

            private void checkExpr(@NotNull RsExpr e, @NotNull PsiElement keyword) {
                if (hasFoldableReturns(e)) {
                    registerProblem(holder, e, keyword);
                }
            }
        };
    }

    @Override
    public boolean isSyntaxOnly() {
        return true;
    }

    private static void registerProblem(@NotNull RsProblemsHolder holder, @NotNull RsExpr expr, @NotNull PsiElement keyword) {
        String keywordName = keyword.getText();
        holder.registerProblem(
            expr,
            keyword.getTextRangeInParent(),
            RsBundle.message("inspection.message.return.can.be.lifted.out", keywordName),
            new LiftReturnOutFix(expr, keywordName)
        );
    }

    private static class LiftReturnOutFix extends RsQuickFixBase<RsExpr> {
        private final String myKeyword;

        LiftReturnOutFix(@NotNull RsExpr element, @NotNull String keyword) {
            super(element);
            this.myKeyword = keyword;
        }

        @NotNull
        @Override
        public String getFamilyName() {
            return RsBundle.message("intention.family.name.lift.return");
        }

        @NotNull
        @Override
        public String getText() {
            return RsBundle.message("intention.name.lift.return.out", myKeyword);
        }

        @Override
        public void invoke(@NotNull Project project, @Nullable Editor editor, @NotNull RsExpr element) {
            List<FoldableElement> foldableReturns = getFoldableReturns(element);
            if (foldableReturns == null) return;
            RsPsiFactory factory = new RsPsiFactory(project);
            for (FoldableElement foldableReturn : foldableReturns) {
                replaceWithTailExpr(foldableReturn.myElementToReplace, factory.createExpression(foldableReturn.myExpr.getText()));
            }
            PsiElement parent = element.getParent();
            if (!(parent instanceof RsRetExpr)) {
                if (parent instanceof RsMatchArm) {
                    addCommaIfNeeded((RsMatchArm) parent, factory);
                }
                element.replace(factory.createRetExpr(element.getText()));
            } else {
                Testmarks.InsideRetExpr.hit();
            }
        }

        private static void addCommaIfNeeded(@NotNull RsMatchArm arm, @NotNull RsPsiFactory psiFactory) {
            if (arm.getComma() != null) return;
            RsMatchExpr matchExpr = RsElementUtil.ancestorStrict(arm, RsMatchExpr.class);
            if (matchExpr == null) return;
            List<RsMatchArm> arms = RsMatchExprUtil.getArms(matchExpr);
            int index = arms.indexOf(arm);
            if (index == -1 || index == arms.size() - 1) return;
            arm.add(psiFactory.createComma());
        }
    }

    public static class Testmarks {
        public static final Testmark InsideRetExpr = new Testmark() {};
    }

    private static void replaceWithTailExpr(@NotNull RsElement element, @NotNull RsExpr expr) {
        if (element instanceof RsExpr) {
            element.replace(expr);
        } else if (element instanceof RsStmt) {
            RsPsiFactory factory = new RsPsiFactory(element.getProject());
            RsExprStmt newStmt = factory.tryCreateExprStmtWithoutSemicolon("()");
            if (newStmt != null) {
                newStmt.getExpr().replace(expr);
                element.replace(newStmt);
            }
        }
    }

    private static class FoldableElement {
        final RsExpr myExpr;
        final RsElement myElementToReplace;

        FoldableElement(@NotNull RsExpr expr, @NotNull RsElement elementToReplace) {
            this.myExpr = expr;
            this.myElementToReplace = elementToReplace;
        }
    }

    private static boolean hasFoldableReturns(@NotNull RsExpr expr) {
        return getFoldableReturns(expr) != null;
    }

    @Nullable
    private static List<FoldableElement> getFoldableReturns(@NotNull RsExpr expr) {
        List<FoldableElement> result = new ArrayList<>();
        if (collectFoldableReturns(expr, result)) {
            return result;
        }
        return null;
    }

    private static boolean collectFoldableReturns(@NotNull RsElement element, @NotNull List<FoldableElement> result) {
        if (element instanceof RsRetExpr) {
            RsRetExpr retExpr = (RsRetExpr) element;
            RsExpr expr = retExpr.getExpr();
            if (expr == null) return false;
            result.add(new FoldableElement(expr, retExpr));
        } else if (element instanceof RsExprStmt) {
            RsExprStmt exprStmt = (RsExprStmt) element;
            if (RsStmtUtil.getHasSemicolon(exprStmt)) {
                RsExpr expr = exprStmt.getExpr();
                if (!(expr instanceof RsRetExpr)) return false;
                RsRetExpr retExpr = (RsRetExpr) expr;
                RsExpr innerExpr = retExpr.getExpr();
                if (innerExpr == null) return false;
                result.add(new FoldableElement(innerExpr, exprStmt));
            } else {
                if (!collectFoldableReturns(exprStmt.getExpr(), result)) return false;
            }
        } else if (element instanceof RsBlock) {
            RsBlock block = (RsBlock) element;
            PsiElement[] children = block.getChildren();
            if (children.length == 0) return false;
            PsiElement lastChild = children[children.length - 1];
            if (!(lastChild instanceof RsElement)) return false;
            if (!collectFoldableReturns((RsElement) lastChild, result)) return false;
        } else if (element instanceof RsBlockExpr) {
            RsBlockExpr blockExpr = (RsBlockExpr) element;
            if (blockExpr.getBlock() == null) return false;
            if (!collectFoldableReturns(blockExpr.getBlock(), result)) return false;
        } else if (element instanceof RsIfExpr) {
            RsIfExpr ifExpr = (RsIfExpr) element;
            if (ifExpr.getBlock() == null) return false;
            if (!collectFoldableReturns(ifExpr.getBlock(), result)) return false;
            RsElseBranch elseBranch = ifExpr.getElseBranch();
            if (elseBranch == null) return false;
            RsIfExpr elseIf = elseBranch.getIfExpr();
            if (elseIf != null) {
                if (!collectFoldableReturns(elseIf, result)) return false;
            } else {
                if (elseBranch.getBlock() == null) return false;
                if (!collectFoldableReturns(elseBranch.getBlock(), result)) return false;
            }
        } else if (element instanceof RsMatchExpr) {
            RsMatchExpr matchExpr = (RsMatchExpr) element;
            RsMatchBody matchBody = matchExpr.getMatchBody();
            if (matchBody == null) return false;
            List<RsMatchArm> arms = matchBody.getMatchArmList();
            if (arms.isEmpty()) return false;
            for (RsMatchArm arm : arms) {
                RsExpr armExpr = arm.getExpr();
                if (armExpr == null) return false;
                if (!collectFoldableReturns(armExpr, result)) return false;
            }
        } else {
            return false;
        }
        return true;
    }
}
