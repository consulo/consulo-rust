/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.intentions;

import consulo.codeEditor.Editor;
import consulo.project.Project;
import consulo.language.psi.PsiComment;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiWhiteSpace;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.rust.RsBundle;
import org.rust.ide.intentions.util.macros.InvokeInside;
import org.rust.lang.core.psi.*;
import org.rust.lang.core.psi.ext.impl.PsiElementExt;
import org.rust.lang.core.psi.ext.RsElement;
import org.rust.lang.core.types.ty.TyNever;
import org.rust.lang.core.types.ty.TyUnit;
import org.rust.lang.core.types.infer.TypeInference;
import org.rust.lang.utils.NegateUtil;
import org.rust.lang.utils.RsBooleanExpUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import org.rust.lang.core.types.RsTypesUtil;
import org.rust.lang.core.psi.ext.impl.RsFunctionUtil;
import org.rust.lang.core.psi.ext.RsFunctionOrLambda;
import org.rust.lang.core.psi.ext.RsLooplikeExpr;
import consulo.localize.LocalizeValue;
import org.rust.lang.core.psi.ext.impl.BinaryOperator;
import org.rust.lang.core.psi.ext.impl.LogicOp;
import org.rust.lang.core.psi.ext.impl.RsBinaryExprUtil;
import org.rust.lang.core.psi.impl.*;

public class InvertIfIntention extends RsElementBaseIntentionAction<InvertIfIntention.Context> {
    @Nonnull
    public consulo.localize.LocalizeValue getFamilyName() {
        return getText();
        }

    @Nonnull
    @Override
    public consulo.localize.LocalizeValue getText() {
        return consulo.localize.LocalizeValue.of(RsBundle.message("intention.name.invert.if.condition"));
        }

    @Nonnull
    @Override
    public InvokeInside getAttributeMacroHandlingStrategy() {
        return InvokeInside.MACRO_CALL;
    }

    public interface Context {
        @Nonnull
        RsExpr getIfCondition();
    }

    public static class ContextWithElse implements Context {
        private final RsIfExpr myIfExpr;
        private final RsExpr myIfCondition;
        private final RsBlock myThenBlock;
        @Nullable
        private final RsBlock myElseBlock;

        public ContextWithElse(@Nonnull RsIfExpr ifExpr, @Nonnull RsExpr ifCondition,
                               @Nonnull RsBlock thenBlock, @Nullable RsBlock elseBlock) {
            myIfExpr = ifExpr;
            myIfCondition = ifCondition;
            myThenBlock = thenBlock;
            myElseBlock = elseBlock;
        }

        @Nonnull
        @Override
        public RsExpr getIfCondition() {
            return myIfCondition;
        }

        @Nonnull
        public RsIfExpr getIfExpr() {
            return myIfExpr;
        }

        @Nonnull
        public RsBlock getThenBlock() {
            return myThenBlock;
        }

        @Nullable
        public RsBlock getElseBlock() {
            return myElseBlock;
        }
    }

    public static class ContextWithoutElse implements Context {
        private final RsIfExpr myIfExpr;
        private final RsExpr myIfCondition;
        private final RsElement myIfStmt;
        private final RsBlock myThenBlock;
        private final List<PsiElement> myThenBlockStmts;
        private final RsBlock myBlock;
        private final List<PsiElement> myNextStmts;

        public ContextWithoutElse(@Nonnull RsIfExpr ifExpr, @Nonnull RsExpr ifCondition,
                                  @Nonnull RsElement ifStmt, @Nonnull RsBlock thenBlock,
                                  @Nonnull List<PsiElement> thenBlockStmts,
                                  @Nonnull RsBlock block, @Nonnull List<PsiElement> nextStmts) {
            myIfExpr = ifExpr;
            myIfCondition = ifCondition;
            myIfStmt = ifStmt;
            myThenBlock = thenBlock;
            myThenBlockStmts = thenBlockStmts;
            myBlock = block;
            myNextStmts = nextStmts;
        }

        @Nonnull
        @Override
        public RsExpr getIfCondition() {
            return myIfCondition;
        }

        @Nonnull
        public RsIfExpr getIfExpr() {
            return myIfExpr;
        }

        @Nonnull
        public RsElement getIfStmt() {
            return myIfStmt;
        }

        @Nonnull
        public RsBlock getThenBlock() {
            return myThenBlock;
        }

        @Nonnull
        public List<PsiElement> getThenBlockStmts() {
            return myThenBlockStmts;
        }

        @Nonnull
        public RsBlock getBlock() {
            return myBlock;
        }

        @Nonnull
        public List<PsiElement> getNextStmts() {
            return myNextStmts;
        }

        public boolean canApplyFix() {
            PsiElement parent = myBlock.getParent();
            boolean hasImplicitReturnOrContinue = parent instanceof RsFunctionOrLambda || parent instanceof RsLooplikeExpr;

            boolean ifDiverges = isDiverges(myThenBlockStmts);
            boolean nextDiverges = isDiverges(myNextStmts) || hasImplicitReturnOrContinue;
            boolean nextIsEmpty = !hasStmts(myNextStmts);
            return nextDiverges && (ifDiverges || nextIsEmpty);
        }
    }

    @Nullable
    @Override
    public Context findApplicableContext(@Nonnull Project project, @Nonnull Editor editor, @Nonnull PsiElement element) {
        RsIfExpr ifExpr = PsiElementExt.ancestorStrict(element, RsIfExpr.class);
        if (ifExpr == null) return null;
        if (element != ifExpr.getIf()) return null;
        RsCondition condition = getSuitableCondition(ifExpr);
        if (condition == null) return null;
        RsExpr conditionExpr = condition.getExpr();
        if (conditionExpr == null) return null;
        RsBlock thenBlock = ifExpr.getBlock();
        if (thenBlock == null) return null;
        RsElseBranch elseBranch = ifExpr.getElseBranch();
        RsBlock elseBlock = elseBranch != null ? elseBranch.getBlock() : null;

        if (elseBlock != null) {
            return new ContextWithElse(ifExpr, conditionExpr, thenBlock, elseBlock);
        } else {
            return createContextWithoutElse(ifExpr, conditionExpr, thenBlock);
        }
    }

    @Nullable
    private ContextWithoutElse createContextWithoutElse(@Nonnull RsIfExpr ifExpr, @Nonnull RsExpr ifCondition, @Nonnull RsBlock thenBlock) {
        List<PsiElement> thenBlockStmts = new ArrayList<>();
        PsiElement sibling = thenBlock.getLbrace().getNextSibling();
        while (sibling != null && sibling != thenBlock.getRbrace()) {
            thenBlockStmts.add(sibling);
            sibling = sibling.getNextSibling();
        }

        RsElement ifStmt;
        RsBlock block;
        PsiElement parent = ifExpr.getParent();
        if (parent instanceof RsExprStmt) {
            PsiElement grandParent = parent.getParent();
            if (!(grandParent instanceof RsBlock)) return null;
            ifStmt = (RsElement) parent;
            block = (RsBlock) grandParent;
        } else if (parent instanceof RsBlock) {
            ifStmt = ifExpr;
            block = (RsBlock) parent;
        } else {
            return null;
        }

        List<PsiElement> nextStmts = new ArrayList<>();
        PsiElement nextSibling = ((PsiElement) ifStmt).getNextSibling();
        while (nextSibling != null && nextSibling != block.getRbrace()) {
            nextStmts.add(nextSibling);
            nextSibling = nextSibling.getNextSibling();
        }

        ContextWithoutElse ctx = new ContextWithoutElse(ifExpr, ifCondition, ifStmt, thenBlock, thenBlockStmts, block, nextStmts);
        if (ctx.canApplyFix()) {
            return ctx;
        }
        return null;
    }

    @Override
    public void invoke(@Nonnull Project project, @Nonnull Editor editor, @Nonnull Context ctx) {
        PsiElement negated = RsBooleanExpUtils.negate(ctx.getIfCondition());
        if (!(negated instanceof RsExpr)) return;
        RsExpr negatedCondition = (RsExpr) negated;

        RsIfExpr newIfExpr;
        if (ctx instanceof ContextWithElse) {
            newIfExpr = handleWithElseBranch(negatedCondition, (ContextWithElse) ctx);
        } else if (ctx instanceof ContextWithoutElse) {
            newIfExpr = handleWithoutElseBranch(negatedCondition, (ContextWithoutElse) ctx);
        } else {
            return;
        }
        if (newIfExpr == null) return;

        RsCondition newCondition = newIfExpr.getCondition();
        if (newCondition == null) return;
        RsExpr newCondExpr = newCondition.getExpr();

        if (newCondExpr instanceof RsUnaryExpr && ((RsUnaryExpr) newCondExpr).getExcl() != null) {
            RsExpr inner = ((RsUnaryExpr) newCondExpr).getExpr();
            if (inner instanceof RsParenExpr) {
                RsExpr parenInner = ((RsParenExpr) inner).getExpr();
                if (parenInner instanceof RsBinaryExpr) {
                    RsBinaryExpr binExpr = (RsBinaryExpr) parenInner;
                    org.rust.lang.core.psi.ext.impl.BinaryOperator opType = org.rust.lang.core.psi.ext.impl.RsBinaryExprUtil.getOperatorType(binExpr);
                    if (opType instanceof org.rust.lang.core.psi.ext.impl.LogicOp) {
                        new DemorgansLawIntention().invoke(project, editor, new DemorgansLawIntention.Context(binExpr, opType));
                    }
                }
            }
        }
    }

    @Nullable
    private RsIfExpr handleWithElseBranch(@Nonnull RsExpr negatedCondition, @Nonnull ContextWithElse ctx) {
        RsPsiFactory psiFactory = new RsPsiFactory(negatedCondition.getProject());
        RsBlock elseBlock = ctx.getElseBlock();
        if (elseBlock == null) return null;
        RsIfExpr newIf = psiFactory.createIfElseExpression(negatedCondition, elseBlock, ctx.getThenBlock());
        return (RsIfExpr) ctx.getIfExpr().replace(newIf);
    }

    @Nullable
    private RsIfExpr handleWithoutElseBranch(@Nonnull RsExpr negatedCondition, @Nonnull ContextWithoutElse ctx) {
        RsPsiFactory factory = new RsPsiFactory(negatedCondition.getProject());
        List<PsiElement> thenCopy = copyList(ctx.getThenBlockStmts());
        List<PsiElement> nextCopy = copyList(ctx.getNextStmts());

        // For the without-else case, we do a simplified swap of branches
        ctx.getIfCondition().replace(negatedCondition);

        // Delete old content
        deleteContinuousChildRange(ctx.getThenBlock(), ctx.getThenBlockStmts());
        deleteContinuousChildRange(ctx.getBlock(), ctx.getNextStmts());

        // Add swapped content
        addAllAfter(ctx.getThenBlock(), nextCopy, ctx.getThenBlock().getLbrace());
        addAllAfter(ctx.getBlock(), thenCopy, (PsiElement) ctx.getIfStmt());

        return ctx.getIfExpr();
    }

    @Nullable
    private RsCondition getSuitableCondition(@Nonnull RsIfExpr ifExpr) {
        RsCondition condition = ifExpr.getCondition();
        if (condition == null) return null;
        RsExpr expr = condition.getExpr();
        if (expr == null) return null;
        if (PsiElementExt.descendantOfTypeOrSelf(expr, RsLetExpr.class) != null) return null;
        return condition;
    }

    private static boolean isDiverges(@Nonnull List<PsiElement> elements) {
        for (PsiElement element : elements) {
            if (elementIsDiverges(element)) return true;
        }
        return false;
    }

    private static boolean elementIsDiverges(@Nonnull PsiElement element) {
        if (element instanceof RsExpr) {
            return RsTypesUtil.getType((RsExpr) element) instanceof TyNever;
        }
        if (element instanceof RsExprStmt) {
            return RsTypesUtil.getType(((RsExprStmt) element).getExpr()) instanceof TyNever;
        }
        return false;
    }

    private static boolean hasStmts(@Nonnull List<PsiElement> elements) {
        for (PsiElement element : elements) {
            if (!(element instanceof PsiWhiteSpace) && !(element instanceof PsiComment)) {
                return true;
            }
        }
        return false;
    }

    @Nonnull
    private static List<PsiElement> copyList(@Nonnull List<PsiElement> elements) {
        List<PsiElement> result = new ArrayList<>(elements.size());
        for (PsiElement element : elements) {
            result.add(element.copy());
        }
        return result;
    }

    private static void deleteContinuousChildRange(@Nonnull RsBlock block, @Nonnull List<PsiElement> stmts) {
        if (!stmts.isEmpty()) {
            block.deleteChildRange(stmts.get(0), stmts.get(stmts.size() - 1));
        }
    }

    private static void addAllAfter(@Nonnull RsElement parent, @Nonnull List<PsiElement> elements, @Nonnull PsiElement anchor) {
        for (int i = elements.size() - 1; i >= 0; i--) {
            parent.addAfter(elements.get(i), anchor);
        }
    }
}
