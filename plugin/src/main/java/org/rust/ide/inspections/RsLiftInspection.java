/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.inspections;

import org.rust.lang.core.psi.ext.impl.RsElementUtil;
import consulo.codeEditor.Editor;
import consulo.project.Project;
import consulo.document.util.TextRange;
import consulo.language.psi.PsiElement;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.rust.RsBundle;
import org.rust.ide.fixes.RsQuickFixBase;
import org.rust.lang.core.psi.*;
import org.rust.lang.core.psi.ext.RsElement;
import org.rust.lang.core.psi.ext.impl.RsMatchExprUtil;
import org.rust.lang.core.psi.ext.impl.RsStmtUtil;
import org.rust.openapiext.Testmark;

import java.util.ArrayList;
import java.util.List;
import org.rust.lang.core.psi.ext.impl.RsFunctionUtil;
import consulo.localize.LocalizeValue;
import consulo.language.editor.rawHighlight.HighlightDisplayLevel;
import consulo.annotation.component.ExtensionImpl;
import org.rust.lang.core.psi.impl.*;

@ExtensionImpl
public class RsLiftInspection extends RsLocalInspectionTool {

    @Override
    public RsVisitor buildVisitor(@Nonnull RsProblemsHolder holder, boolean isOnTheFly) {
        return new RsWithMacrosInspectionVisitor() {
            @Override
            public void visitIfExpr(@Nonnull RsIfExpr o) {
                if (o.getParent() instanceof RsElseBranch) return;
                checkExpr(o, o.getIf());
            }

            @Override
            public void visitMatchExpr(@Nonnull RsMatchExpr o) {
                checkExpr(o, o.getMatch());
            }

            private void checkExpr(@Nonnull RsExpr e, @Nonnull PsiElement keyword) {
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

    private static void registerProblem(@Nonnull RsProblemsHolder holder, @Nonnull RsExpr expr, @Nonnull PsiElement keyword) {
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

        LiftReturnOutFix(@Nonnull RsExpr element, @Nonnull String keyword) {
            super(element);
            this.myKeyword = keyword;
        }

        @Nonnull
        @Override
        public consulo.localize.LocalizeValue getFamilyName() {
            return consulo.localize.LocalizeValue.of(RsBundle.message("intention.family.name.lift.return"));
            }

        @Nonnull
        @Override
        public consulo.localize.LocalizeValue getText() {
            return consulo.localize.LocalizeValue.of(RsBundle.message("intention.name.lift.return.out", myKeyword));
            }

        @Override
        public void invoke(@Nonnull Project project, @Nullable Editor editor, @Nonnull RsExpr element) {
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

        private static void addCommaIfNeeded(@Nonnull RsMatchArm arm, @Nonnull RsPsiFactory psiFactory) {
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

    private static void replaceWithTailExpr(@Nonnull RsElement element, @Nonnull RsExpr expr) {
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

        FoldableElement(@Nonnull RsExpr expr, @Nonnull RsElement elementToReplace) {
            this.myExpr = expr;
            this.myElementToReplace = elementToReplace;
        }
    }

    private static boolean hasFoldableReturns(@Nonnull RsExpr expr) {
        return getFoldableReturns(expr) != null;
    }

    @Nullable
    private static List<FoldableElement> getFoldableReturns(@Nonnull RsExpr expr) {
        List<FoldableElement> result = new ArrayList<>();
        if (collectFoldableReturns(expr, result)) {
            return result;
        }
        return null;
    }

    private static boolean collectFoldableReturns(@Nonnull RsElement element, @Nonnull List<FoldableElement> result) {
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

    @Override
    @jakarta.annotation.Nonnull
    public consulo.localize.LocalizeValue getDisplayName() {
        return consulo.localize.LocalizeValue.of(org.rust.RsBundle.message("inspection.rs.lift.display.name"));
    }

    @Override
    @jakarta.annotation.Nonnull
    public consulo.localize.LocalizeValue getGroupDisplayName() {
        return consulo.localize.LocalizeValue.of(org.rust.RsBundle.message("rust"));
    }

    @Nonnull
    @Override
    public HighlightDisplayLevel getDefaultLevel() {
        return HighlightDisplayLevel.WEAK_WARNING;
    }
}
