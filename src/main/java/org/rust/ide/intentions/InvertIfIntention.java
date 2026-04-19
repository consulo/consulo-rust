/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.intentions;

import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiComment;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiWhiteSpace;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.rust.RsBundle;
import org.rust.ide.intentions.util.macros.InvokeInside;
import org.rust.lang.core.psi.*;
import org.rust.lang.core.psi.ext.PsiElementExt;
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
import org.rust.lang.core.psi.ext.RsFunctionUtil;
import org.rust.lang.core.psi.ext.RsFunctionOrLambda;
import org.rust.lang.core.psi.ext.RsLooplikeExpr;

public class InvertIfIntention extends RsElementBaseIntentionAction<InvertIfIntention.Context> {
    @NotNull
    @Override
    public String getFamilyName() {
        return getText();
    }

    @NotNull
    @Override
    public String getText() {
        return RsBundle.message("intention.name.invert.if.condition");
    }

    @NotNull
    @Override
    public InvokeInside getAttributeMacroHandlingStrategy() {
        return InvokeInside.MACRO_CALL;
    }

    public interface Context {
        @NotNull
        RsExpr getIfCondition();
    }

    public static class ContextWithElse implements Context {
        private final RsIfExpr myIfExpr;
        private final RsExpr myIfCondition;
        private final RsBlock myThenBlock;
        @Nullable
        private final RsBlock myElseBlock;

        public ContextWithElse(@NotNull RsIfExpr ifExpr, @NotNull RsExpr ifCondition,
                               @NotNull RsBlock thenBlock, @Nullable RsBlock elseBlock) {
            myIfExpr = ifExpr;
            myIfCondition = ifCondition;
            myThenBlock = thenBlock;
            myElseBlock = elseBlock;
        }

        @NotNull
        @Override
        public RsExpr getIfCondition() {
            return myIfCondition;
        }

        @NotNull
        public RsIfExpr getIfExpr() {
            return myIfExpr;
        }

        @NotNull
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

        public ContextWithoutElse(@NotNull RsIfExpr ifExpr, @NotNull RsExpr ifCondition,
                                  @NotNull RsElement ifStmt, @NotNull RsBlock thenBlock,
                                  @NotNull List<PsiElement> thenBlockStmts,
                                  @NotNull RsBlock block, @NotNull List<PsiElement> nextStmts) {
            myIfExpr = ifExpr;
            myIfCondition = ifCondition;
            myIfStmt = ifStmt;
            myThenBlock = thenBlock;
            myThenBlockStmts = thenBlockStmts;
            myBlock = block;
            myNextStmts = nextStmts;
        }

        @NotNull
        @Override
        public RsExpr getIfCondition() {
            return myIfCondition;
        }

        @NotNull
        public RsIfExpr getIfExpr() {
            return myIfExpr;
        }

        @NotNull
        public RsElement getIfStmt() {
            return myIfStmt;
        }

        @NotNull
        public RsBlock getThenBlock() {
            return myThenBlock;
        }

        @NotNull
        public List<PsiElement> getThenBlockStmts() {
            return myThenBlockStmts;
        }

        @NotNull
        public RsBlock getBlock() {
            return myBlock;
        }

        @NotNull
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
    public Context findApplicableContext(@NotNull Project project, @NotNull Editor editor, @NotNull PsiElement element) {
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
    private ContextWithoutElse createContextWithoutElse(@NotNull RsIfExpr ifExpr, @NotNull RsExpr ifCondition, @NotNull RsBlock thenBlock) {
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
    public void invoke(@NotNull Project project, @NotNull Editor editor, @NotNull Context ctx) {
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
                    org.rust.lang.core.psi.ext.BinaryOperator opType = org.rust.lang.core.psi.ext.RsBinaryExprUtil.getOperatorType(binExpr);
                    if (opType instanceof org.rust.lang.core.psi.ext.LogicOp) {
                        new DemorgansLawIntention().invoke(project, editor, new DemorgansLawIntention.Context(binExpr, opType));
                    }
                }
            }
        }
    }

    @Nullable
    private RsIfExpr handleWithElseBranch(@NotNull RsExpr negatedCondition, @NotNull ContextWithElse ctx) {
        RsPsiFactory psiFactory = new RsPsiFactory(negatedCondition.getProject());
        RsBlock elseBlock = ctx.getElseBlock();
        if (elseBlock == null) return null;
        RsIfExpr newIf = psiFactory.createIfElseExpression(negatedCondition, elseBlock, ctx.getThenBlock());
        return (RsIfExpr) ctx.getIfExpr().replace(newIf);
    }

    @Nullable
    private RsIfExpr handleWithoutElseBranch(@NotNull RsExpr negatedCondition, @NotNull ContextWithoutElse ctx) {
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
    private RsCondition getSuitableCondition(@NotNull RsIfExpr ifExpr) {
        RsCondition condition = ifExpr.getCondition();
        if (condition == null) return null;
        RsExpr expr = condition.getExpr();
        if (expr == null) return null;
        if (PsiElementExt.descendantOfTypeOrSelf(expr, RsLetExpr.class) != null) return null;
        return condition;
    }

    private static boolean isDiverges(@NotNull List<PsiElement> elements) {
        for (PsiElement element : elements) {
            if (elementIsDiverges(element)) return true;
        }
        return false;
    }

    private static boolean elementIsDiverges(@NotNull PsiElement element) {
        if (element instanceof RsExpr) {
            return RsTypesUtil.getType((RsExpr) element) instanceof TyNever;
        }
        if (element instanceof RsExprStmt) {
            return RsTypesUtil.getType(((RsExprStmt) element).getExpr()) instanceof TyNever;
        }
        return false;
    }

    private static boolean hasStmts(@NotNull List<PsiElement> elements) {
        for (PsiElement element : elements) {
            if (!(element instanceof PsiWhiteSpace) && !(element instanceof PsiComment)) {
                return true;
            }
        }
        return false;
    }

    @NotNull
    private static List<PsiElement> copyList(@NotNull List<PsiElement> elements) {
        List<PsiElement> result = new ArrayList<>(elements.size());
        for (PsiElement element : elements) {
            result.add(element.copy());
        }
        return result;
    }

    private static void deleteContinuousChildRange(@NotNull RsBlock block, @NotNull List<PsiElement> stmts) {
        if (!stmts.isEmpty()) {
            block.deleteChildRange(stmts.get(0), stmts.get(stmts.size() - 1));
        }
    }

    private static void addAllAfter(@NotNull RsElement parent, @NotNull List<PsiElement> elements, @NotNull PsiElement anchor) {
        for (int i = elements.size() - 1; i >= 0; i--) {
            parent.addAfter(elements.get(i), anchor);
        }
    }
}
