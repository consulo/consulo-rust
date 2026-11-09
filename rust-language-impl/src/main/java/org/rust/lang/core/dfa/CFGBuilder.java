/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.dfa;


import consulo.language.psi.PsiElement;
import consulo.language.psi.util.PsiTreeUtil;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.rust.lang.core.psi.*;
import org.rust.lang.core.psi.ext.*;
import org.rust.lang.core.types.regions.Scope;
import org.rust.lang.core.types.regions.ScopeTree;
import org.rust.lang.core.types.ty.TyNever;
import org.rust.lang.core.types.ty.TyPrimitive;
import consulo.application.progress.ProgressManager;
import consulo.language.controlFlow.ControlFlowBuilder;
import consulo.language.controlFlow.ControlFlow;

import java.util.*;
import org.rust.lang.core.macros.MacroExpansion;
import org.rust.lang.core.psi.ext.impl.*;

public class CFGBuilder extends RsVisitor {

    public static class BlockScope {
        @Nonnull public final RsBlockExpr blockExpr;
        @Nonnull public final CFGInstruction breakNode;

        public BlockScope(@Nonnull RsBlockExpr blockExpr, @Nonnull CFGInstruction breakNode) {
            this.blockExpr = blockExpr;
            this.breakNode = breakNode;
        }
    }

    public static class LoopScope {
        @Nonnull public final RsLooplikeExpr loop;
        @Nonnull public final CFGInstruction continueNode;
        @Nonnull public final CFGInstruction breakNode;

        public LoopScope(@Nonnull RsLooplikeExpr loop, @Nonnull CFGInstruction continueNode, @Nonnull CFGInstruction breakNode) {
            this.loop = loop;
            this.continueNode = continueNode;
            this.breakNode = breakNode;
        }
    }

    public enum ScopeCFKind {
        Break, Continue;

        @Nonnull
        public CFGInstruction select(@Nonnull CFGInstruction breakNode, @Nonnull CFGInstruction continueNode) {
            return this == Break ? breakNode : continueNode;
        }
    }

    @Nonnull private final ScopeTree regionScopeTree;
    @Nonnull private final ControlFlowBuilder flow = new ControlFlowBuilder();
    /** the scopes an edge exits (loops, blocks), by {@link ControlFlowGraph#edgeKey}; edges exiting none are absent */
    @Nonnull private final Map<Long, List<RsElement>> exitingScopes = new HashMap<>();
    @Nonnull public final CFGInstruction entry;
    @Nonnull public final CFGInstruction exit;
    @Nonnull public final CFGInstruction termination;

    @Nullable private CFGInstruction result;
    @Nonnull private final Deque<CFGInstruction> preds = new ArrayDeque<>();
    @Nonnull private final Deque<LoopScope> loopScopes = new ArrayDeque<>();
    @Nonnull private final Deque<BlockScope> breakableBlockScopes = new ArrayDeque<>();

    public CFGBuilder(@Nonnull ScopeTree regionScopeTree) {
        this.regionScopeTree = regionScopeTree;
        this.entry = addNode(CFGNodeData.Entry.INSTANCE);
        this.exit = addNode(CFGNodeData.Exit.INSTANCE);
        this.termination = addNode(CFGNodeData.Termination.INSTANCE);
    }

    @Nonnull
    public ControlFlow complete() {
        return flow.completeControlFlow();
    }

    @Nonnull
    public Map<Long, List<RsElement>> getExitingScopes() {
        return exitingScopes;
    }

    @Nonnull
    private CFGInstruction getPred() {
        return preds.peek();
    }

    private void finishWith(@Nonnull CFGInstruction value) {
        result = value;
    }

    private void finishWithAstNode(@Nonnull RsElement element, @Nonnull CFGInstruction... preds) {
        result = addAstNode(element, preds);
    }

    private void finishWithUnreachableNode(@Nullable CFGInstruction end) {
        if (end != null) {
            addTerminationEdge(end);
        }
        result = addUnreachableNode();
    }

    @Nonnull
    private CFGInstruction addAstNode(@Nonnull RsElement element, @Nonnull CFGInstruction... preds) {
        return addNode(new CFGNodeData.AST(element), preds);
    }

    @Nonnull
    private CFGInstruction addDummyNode(@Nonnull CFGInstruction... preds) {
        return addNode(CFGNodeData.Dummy.INSTANCE, preds);
    }

    @Nonnull
    private CFGInstruction addUnreachableNode() {
        return addNode(CFGNodeData.Unreachable.INSTANCE);
    }

    @Nonnull
    private CFGInstruction addNode(@Nonnull CFGNodeData data, @Nonnull CFGInstruction... preds) {
        CFGInstruction newNode = new CFGInstruction(flow, data);
        flow.flowAbrupted();
        flow.addNode(newNode);
        for (CFGInstruction p : preds) {
            addContainedEdge(p, newNode);
        }
        return newNode;
    }

    public void addContainedEdge(@Nonnull CFGInstruction source, @Nonnull CFGInstruction target) {
        addEdge(source, target, Collections.emptyList());
    }

    private void addEdge(@Nonnull CFGInstruction source, @Nonnull CFGInstruction target, @Nonnull List<RsElement> scopes) {
        flow.addEdge(source, target);
        if (!scopes.isEmpty()) {
            exitingScopes.computeIfAbsent(ControlFlowGraph.edgeKey(source, target), k -> new ArrayList<>()).addAll(scopes);
        }
    }

    private void addReturningEdge(@Nonnull CFGInstruction fromNode) {
        addEdge(fromNode, exit, enclosingLoops());
    }

    private void addTerminationEdge(@Nonnull CFGInstruction fromNode) {
        addEdge(fromNode, termination, enclosingLoops());
    }

    @Nonnull
    private List<RsElement> enclosingLoops() {
        List<RsElement> scopes = new ArrayList<>();
        for (LoopScope ls : loopScopes) {
            scopes.add(ls.loop);
        }
        return scopes;
    }

    private void addExitingEdge(@Nonnull RsExpr fromExpr, @Nonnull CFGInstruction fromNode,
                                @Nonnull Scope targetScope, @Nonnull CFGInstruction toNode) {
        List<RsElement> exitingScopes = new ArrayList<>();
        Scope scope = new Scope.Node(fromExpr);
        while (!scope.equals(targetScope)) {
            exitingScopes.add(scope.getElement());
            Scope enclosing = regionScopeTree.getEnclosingScope(scope);
            if (enclosing == null) break;
            scope = enclosing;
        }
        addEdge(fromNode, toNode, exitingScopes);
    }

    @Nullable
    private Object[] findScopeEdge(@Nonnull RsExpr expr, @Nullable RsLabel label, @Nonnull ScopeCFKind kind) {
        if (label != null) {
            Object labelDeclaration = label.getReference().resolve();
            if (labelDeclaration == null) return null;

            for (BlockScope bs : breakableBlockScopes) {
                if (labelDeclaration == bs.blockExpr.getLabelDecl()) {
                    return new Object[]{new Scope.Node(bs.blockExpr), bs.breakNode};
                }
            }
            for (LoopScope ls : loopScopes) {
                if (labelDeclaration == ls.loop.getLabelDecl()) {
                    CFGInstruction node = kind.select(ls.breakNode, ls.continueNode);
                    return new Object[]{new Scope.Node(ls.loop), node};
                }
            }
        } else {
            RsBlock exprBlock = null;
            for (PsiElement ctx : RsElementUtil.getContexts(expr)) {
                if (ctx instanceof RsLooplikeExpr) {
                    exprBlock = ((RsLooplikeExpr) ctx).getBlock();
                    break;
                }
            }

            for (LoopScope ls : loopScopes) {
                if (ls.loop.getBlock() == exprBlock) {
                    CFGInstruction node = kind.select(ls.breakNode, ls.continueNode);
                    return new Object[]{new Scope.Node(ls.loop), node};
                }
            }
        }
        return null;
    }

    @Nonnull
    private CFGInstruction straightLine(@Nonnull RsExpr expr, @Nonnull CFGInstruction pred, @Nonnull List<RsExpr> subExprs) {
        CFGInstruction subExprsExit = pred;
        for (RsExpr subExpr : subExprs) {
            subExprsExit = process(subExpr, subExprsExit);
        }
        return addAstNode(expr, subExprsExit);
    }

    @Nonnull
    public CFGInstruction process(@Nullable RsElement element, @Nonnull CFGInstruction pred) {
        if (element == null) return pred;
        ProgressManager.checkCanceled();

        if (element instanceof RsDocAndAttributeOwner && !RsDocAndAttributeOwnerUtil.existsAfterExpansionSelf((RsDocAndAttributeOwner) element)) {
            return pred;
        }

        result = null;
        int oldPredsSize = preds.size();
        preds.push(pred);
        element.accept(this);
        preds.pop();
        assert preds.size() == oldPredsSize;

        if (result == null) throw new IllegalStateException("Processing ended inconclusively");
        return result;
    }

    @Nonnull
    private CFGInstruction processSubPats(@Nonnull RsPat pat, @Nonnull List<? extends RsPat> subPats) {
        CFGInstruction patsExit = getPred();
        for (RsPat subPat : subPats) {
            patsExit = process(subPat, patsExit);
        }
        return addAstNode(pat, patsExit);
    }

    @Nonnull
    private CFGInstruction processConditionPats(@Nonnull RsLetExpr letExpr, @Nonnull CFGInstruction pred) {
        List<RsPat> pats = RsLetExprUtil.getPatList(letExpr);
        if (pats == null) return pred;
        CFGInstruction conditionExit = addDummyNode();
        for (RsPat pat : pats) {
            CFGInstruction patExit = process(pat, pred);
            addContainedEdge(patExit, conditionExit);
        }
        return conditionExit;
    }

    @Nonnull
    private CFGInstruction processCall(@Nonnull RsExpr callExpr, @Nullable RsExpr funcOrReceiver, @Nonnull List<? extends RsExpr> args) {
        CFGInstruction funcOrReceiverExit = process(funcOrReceiver, getPred());
        List<RsExpr> argsCopy = new ArrayList<>(args);
        CFGInstruction callExit = straightLine(callExpr, funcOrReceiverExit, argsCopy);
        if (RsExprUtil.getType(callExpr) instanceof TyNever) {
            addTerminationEdge(callExit);
            return addUnreachableNode();
        }
        return callExit;
    }

    // Visitor methods

    @Override
    public void visitBlock(@Nonnull RsBlock block) {
        RsBlockUtil.ExpandedStmtsAndTailExpr expanded = RsBlockUtil.getExpandedStmtsAndTailExpr(block);
        CFGInstruction stmtsExit = getPred();
        for (RsElement stmt : expanded.getStmts()) {
            stmtsExit = process(stmt, stmtsExit);
        }
        RsExpr tailExpr = expanded.getTailExpr();
        if (tailExpr != null) {
            CFGInstruction exprExit = process(tailExpr, stmtsExit);
            finishWithAstNode(block, exprExit);
        } else {
            finishWithAstNode(block, stmtsExit);
        }
    }

    @Override
    public void visitLetDecl(@Nonnull RsLetDecl letDecl) {
        CFGInstruction initExit = process(letDecl.getExpr(), getPred());
        CFGInstruction exitNode = process(letDecl.getPat(), initExit);

        RsLetElseBranch elseBranch = letDecl.getLetElseBranch();
        if (elseBranch != null) {
            CFGInstruction elseBranchExit = process(elseBranch.getBlock(), initExit);
            finishWithAstNode(letDecl, exitNode, elseBranchExit);
        } else {
            finishWithAstNode(letDecl, exitNode);
        }
    }

    @Override
    public void visitLetExpr(@Nonnull RsLetExpr letExpr) {
        CFGInstruction initExit = process(letExpr.getExpr(), getPred());
        CFGInstruction exitNode = process(letExpr.getPat(), initExit);
        finishWithAstNode(letExpr, exitNode);
    }

    @Override
    public void visitNamedFieldDecl(@Nonnull RsNamedFieldDecl fieldDecl) {
        finishWith(getPred());
    }

    @Override
    public void visitLabelDecl(@Nonnull RsLabelDecl labelDecl) {
        finishWith(getPred());
    }

    @Override
    public void visitExprStmt(@Nonnull RsExprStmt exprStmt) {
        CFGInstruction exprExit = process(exprStmt.getExpr(), getPred());
        finishWithAstNode(exprStmt, exprExit);
    }

    @Override
    public void visitPatIdent(@Nonnull RsPatIdent patIdent) {
        CFGInstruction subPatExit = process(patIdent.getPat(), getPred());
        CFGInstruction bindingExit = process(patIdent.getPatBinding(), subPatExit);
        finishWithAstNode(patIdent, bindingExit);
    }

    @Override
    public void visitPatBinding(@Nonnull RsPatBinding patBinding) {
        finishWithAstNode(patBinding, getPred());
    }

    @Override
    public void visitPatRange(@Nonnull RsPatRange patRange) {
        finishWithAstNode(patRange, getPred());
    }

    @Override
    public void visitPatConst(@Nonnull RsPatConst patConst) {
        finishWithAstNode(patConst, getPred());
    }

    @Override
    public void visitPatWild(@Nonnull RsPatWild patWild) {
        finishWithAstNode(patWild, getPred());
    }

    @Override
    public void visitPathExpr(@Nonnull RsPathExpr pathExpr) {
        finishWithAstNode(pathExpr, getPred());
    }

    @Override
    public void visitMacroBodyIdent(@Nonnull RsMacroBodyIdent macroBodyIdent) {
        finishWithAstNode(macroBodyIdent, getPred());
    }

    @Override
    public void visitMacroExpr(@Nonnull RsMacroExpr macroExpr) {
        CFGInstruction macroCallExit = process(macroExpr.getMacroCall(), getPred());
        if (macroCallExit != getPred() && RsExprUtil.getType(macroExpr) instanceof TyNever) {
            finishWithUnreachableNode(macroCallExit);
        } else {
            finishWith(macroCallExit);
        }
    }

    @Override
    public void visitMacroCall(@Nonnull RsMacroCall macroCall) {
        CFGInstruction subElementsExit;
        Object argument = RsMacroCallUtil.getMacroArgumentElement(macroCall);

        CFGInstruction subExprsExit = null;
        if (argument instanceof RsExprMacroArgument) {
            RsExpr expr = ((RsExprMacroArgument) argument).getExpr();
            if (expr != null) subExprsExit = process(expr, getPred());
        } else if (argument instanceof RsIncludeMacroArgument) {
            RsExpr expr = ((RsIncludeMacroArgument) argument).getExpr();
            if (expr != null) subExprsExit = process(expr, getPred());
        } else if (argument instanceof RsConcatMacroArgument) {
            CFGInstruction acc = getPred();
            for (RsExpr e : ((RsConcatMacroArgument) argument).getExprList()) acc = process(e, acc);
            subExprsExit = acc;
        } else if (argument instanceof RsEnvMacroArgument) {
            CFGInstruction acc = getPred();
            for (RsExpr e : ((RsEnvMacroArgument) argument).getExprList()) acc = process(e, acc);
            subExprsExit = acc;
        } else if (argument instanceof RsVecMacroArgument) {
            CFGInstruction acc = getPred();
            for (RsExpr e : ((RsVecMacroArgument) argument).getExprList()) acc = process(e, acc);
            subExprsExit = acc;
        } else if (argument instanceof RsFormatMacroArgument) {
            Object expansion = RsMacroCallUtil.getExpansion(macroCall);
            if (expansion != null) {
                CFGInstruction acc = getPred();
                for (RsElement el : ((org.rust.lang.core.macros.MacroExpansion) expansion).getElements()) acc = process(el, acc);
                subExprsExit = acc;
            } else {
                CFGInstruction acc = getPred();
                for (RsFormatMacroArg arg : ((RsFormatMacroArgument) argument).getFormatMacroArgList()) {
                    acc = process(arg.getExpr(), acc);
                }
                subExprsExit = acc;
            }
        } else if (argument instanceof RsAssertMacroArgument) {
            CFGInstruction acc = getPred();
            RsExpr e = ((RsAssertMacroArgument) argument).getExpr();
            if (e != null) acc = process(e, acc);
            for (RsFormatMacroArg arg : ((RsAssertMacroArgument) argument).getFormatMacroArgList()) {
                acc = process(arg.getExpr(), acc);
            }
            subExprsExit = acc;
        } else if (argument instanceof RsAsmMacroArgument) {
            // TODO: Handle this case when type inference is implemented for asm! macro calls
        } else if (argument instanceof RsMacroArgument) {
            Object expansion = RsMacroCallUtil.getExpansion(macroCall);
            if (expansion != null) {
                CFGInstruction acc = getPred();
                for (RsElement el : ((org.rust.lang.core.macros.MacroExpansion) expansion).getElements()) acc = process(el, acc);
                subExprsExit = acc;
            }
        }

        if (subExprsExit == null) {
            Collection<RsElement> subPathsIdents = (Collection<RsElement>) (Collection<?>) PsiTreeUtil.findChildrenOfAnyType(
                macroCall, RsPathExpr.class, RsMacroBodyIdent.class
            );
            CFGInstruction acc = getPred();
            for (RsElement el : subPathsIdents) acc = process(el, acc);
            subElementsExit = acc;
        } else {
            subElementsExit = subExprsExit;
        }

        finishWithAstNode(macroCall, subElementsExit);
    }

    @Override
    public void visitRangeExpr(@Nonnull RsRangeExpr rangeExpr) {
        finishWith(straightLine(rangeExpr, getPred(), rangeExpr.getExprList()));
    }

    @Override
    public void visitPatTup(@Nonnull RsPatTup patTup) {
        finishWith(processSubPats(patTup, patTup.getPatList()));
    }

    @Override
    public void visitPatTupleStruct(@Nonnull RsPatTupleStruct patTupleStruct) {
        finishWith(processSubPats(patTupleStruct, patTupleStruct.getPatList()));
    }

    @Override
    public void visitPatStruct(@Nonnull RsPatStruct patStruct) {
        CFGInstruction patFieldsExit = getPred();
        for (RsPatField patField : patStruct.getPatFieldList()) {
            patFieldsExit = process(patField, patFieldsExit);
        }
        finishWithAstNode(patStruct, patFieldsExit);
    }

    @Override
    public void visitPatField(@Nonnull RsPatField patField) {
        RsPatFieldFull patFieldFull = patField.getPatFieldFull();
        CFGInstruction subPatExit;
        if (patFieldFull != null) {
            subPatExit = process(patFieldFull.getPat(), getPred());
        } else {
            subPatExit = process(patField.getPatBinding(), getPred());
        }
        finishWithAstNode(patField, subPatExit);
    }

    @Override
    public void visitPatSlice(@Nonnull RsPatSlice patSlice) {
        finishWith(processSubPats(patSlice, patSlice.getPatList()));
    }

    @Override
    public void visitBlockExpr(@Nonnull RsBlockExpr blockExpr) {
        RsLabelDecl labelDeclaration = blockExpr.getLabelDecl();
        boolean isAsync = RsBlockExprUtil.isAsync(blockExpr);
        CFGInstruction exprExit = addAstNode(blockExpr);

        if (labelDeclaration != null) {
            breakableBlockScopes.push(new BlockScope(blockExpr, exprExit));
            RsBlockUtil.ExpandedStmtsAndTailExpr expanded = RsBlockUtil.getExpandedStmtsAndTailExpr(blockExpr.getBlock());
            CFGInstruction stmtsExit = getPred();
            for (RsElement stmt : expanded.getStmts()) {
                stmtsExit = process(stmt, stmtsExit);
            }
            CFGInstruction blockExprExit = process(expanded.getTailExpr(), stmtsExit);
            addContainedEdge(blockExprExit, exprExit);
            breakableBlockScopes.pop();
        } else {
            CFGInstruction blockExit = process(blockExpr.getBlock(), getPred());
            addContainedEdge(blockExit, exprExit);
        }

        if (isAsync) {
            addContainedEdge(getPred(), exprExit);
        }
        finishWith(exprExit);
    }

    @Override
    public void visitIfExpr(@Nonnull RsIfExpr ifExpr) {
        RsCondition condition = ifExpr.getCondition();
        RsExpr expr = condition != null ? condition.getExpr() : null;

        CFGInstruction exprExit;
        CFGInstruction conditionPatsExit;
        if (expr instanceof RsLetExpr) {
            RsLetExpr letExpr = (RsLetExpr) expr;
            exprExit = process(letExpr.getExpr(), getPred());
            conditionPatsExit = processConditionPats(letExpr, exprExit);
        } else {
            exprExit = process(expr, getPred());
            conditionPatsExit = exprExit;
        }

        CFGInstruction thenExit = process(ifExpr.getBlock(), conditionPatsExit);
        RsElseBranch elseBranch = ifExpr.getElseBranch();

        if (elseBranch != null) {
            RsBlock elseBranchBlock = elseBranch.getBlock();
            if (elseBranchBlock != null) {
                CFGInstruction elseExit = process(elseBranchBlock, exprExit);
                finishWithAstNode(ifExpr, thenExit, elseExit);
            } else {
                CFGInstruction nestedIfExit = process(elseBranch.getIfExpr(), exprExit);
                finishWithAstNode(ifExpr, thenExit, nestedIfExit);
            }
        } else {
            finishWithAstNode(ifExpr, exprExit, thenExit);
        }
    }

    @Override
    public void visitWhileExpr(@Nonnull RsWhileExpr whileExpr) {
        CFGInstruction loopBack = addDummyNode(getPred());
        CFGInstruction whileExprExit = addAstNode(whileExpr);
        LoopScope loopScope = new LoopScope(whileExpr, loopBack, whileExprExit);

        loopScopes.push(loopScope);
        RsCondition condition = whileExpr.getCondition();
        RsExpr expr = condition != null ? condition.getExpr() : null;

        CFGInstruction exprExit;
        if (expr instanceof RsLetExpr) {
            RsLetExpr letExpr = (RsLetExpr) expr;
            exprExit = process(letExpr.getExpr(), loopBack);
            addContainedEdge(exprExit, whileExprExit);
            exprExit = processConditionPats(letExpr, exprExit);
        } else {
            exprExit = process(expr, loopBack);
            addContainedEdge(exprExit, whileExprExit);
        }

        CFGInstruction bodyExit = process(whileExpr.getBlock(), exprExit);
        addContainedEdge(bodyExit, loopBack);
        loopScopes.pop();

        finishWith(whileExprExit);
    }

    @Override
    public void visitLoopExpr(@Nonnull RsLoopExpr loopExpr) {
        CFGInstruction loopBack = addDummyNode(getPred());
        CFGInstruction exprExit = addAstNode(loopExpr);
        LoopScope loopScope = new LoopScope(loopExpr, loopBack, exprExit);

        loopScopes.push(loopScope);
        CFGInstruction bodyExit = process(loopExpr.getBlock(), loopBack);
        addContainedEdge(bodyExit, loopBack);
        loopScopes.pop();

        addTerminationEdge(loopBack);
        finishWith(exprExit);
    }

    @Override
    public void visitForExpr(@Nonnull RsForExpr forExpr) {
        CFGInstruction exprExit = addAstNode(forExpr);
        CFGInstruction iterExprExit = process(forExpr.getExpr(), getPred());
        CFGInstruction loopBack = addDummyNode(iterExprExit);
        addContainedEdge(loopBack, exprExit);

        LoopScope loopScope = new LoopScope(forExpr, loopBack, exprExit);
        loopScopes.push(loopScope);
        CFGInstruction patExit = process(forExpr.getPat(), loopBack);
        CFGInstruction bodyExit = process(forExpr.getBlock(), patExit);
        addContainedEdge(bodyExit, loopBack);
        loopScopes.pop();

        finishWith(exprExit);
    }

    @Override
    public void visitBinaryExpr(@Nonnull RsBinaryExpr binaryExpr) {
        if (RsBinaryOpUtil.getOperatorType(binaryExpr.getBinaryOp()) instanceof LogicOp) {
            CFGInstruction leftExit = process(binaryExpr.getLeft(), getPred());
            CFGInstruction rightExit = process(binaryExpr.getRight(), leftExit);
            finishWithAstNode(binaryExpr, leftExit, rightExit);
        } else {
            if (RsExprUtil.getType(binaryExpr.getLeft()) instanceof TyPrimitive) {
                List<RsExpr> subs = new ArrayList<>();
                subs.add(binaryExpr.getLeft());
                RsExpr right = binaryExpr.getRight();
                if (right != null) subs.add(right);
                finishWith(straightLine(binaryExpr, getPred(), subs));
            } else {
                List<RsExpr> args = new ArrayList<>();
                RsExpr right = binaryExpr.getRight();
                if (right != null) args.add(right);
                finishWith(processCall(binaryExpr, binaryExpr.getLeft(), args));
            }
        }
    }

    @Override
    public void visitRetExpr(@Nonnull RsRetExpr retExpr) {
        CFGInstruction valueExit = process(retExpr.getExpr(), getPred());
        CFGInstruction returnExit = addAstNode(retExpr, valueExit);
        addReturningEdge(returnExit);
        finishWithUnreachableNode(null);
    }

    @Override
    public void visitBreakExpr(@Nonnull RsBreakExpr breakExpr) {
        CFGInstruction exprExit = process(breakExpr.getExpr(), getPred());
        Object[] scopeEdge = findScopeEdge(breakExpr, breakExpr.getLabel(), ScopeCFKind.Break);
        if (scopeEdge == null) {
            finishWithUnreachableNode(exprExit);
            return;
        }
        Scope targetScope = (Scope) scopeEdge[0];
        CFGInstruction breakDestination = (CFGInstruction) scopeEdge[1];
        CFGInstruction breakExit = addAstNode(breakExpr, exprExit);
        addExitingEdge(breakExpr, breakExit, targetScope, breakDestination);
        finishWithUnreachableNode(breakExit);
    }

    @Override
    public void visitContExpr(@Nonnull RsContExpr contExpr) {
        CFGInstruction contExit = addAstNode(contExpr, getPred());
        Object[] scopeEdge = findScopeEdge(contExpr, contExpr.getLabel(), ScopeCFKind.Continue);
        if (scopeEdge == null) {
            finishWithUnreachableNode(contExit);
            return;
        }
        Scope targetScope = (Scope) scopeEdge[0];
        CFGInstruction contDestination = (CFGInstruction) scopeEdge[1];
        addExitingEdge(contExpr, contExit, targetScope, contDestination);
        finishWithUnreachableNode(contExit);
    }

    @Override
    public void visitArrayExpr(@Nonnull RsArrayExpr arrayExpr) {
        finishWith(straightLine(arrayExpr, getPred(), arrayExpr.getExprList()));
    }

    @Override
    public void visitCallExpr(@Nonnull RsCallExpr callExpr) {
        finishWith(processCall(callExpr, callExpr.getExpr(), callExpr.getValueArgumentList().getExprList()));
    }

    @Override
    public void visitIndexExpr(@Nonnull RsIndexExpr indexExpr) {
        List<RsExpr> exprs = indexExpr.getExprList();
        RsExpr first = exprs.isEmpty() ? null : exprs.get(0);
        List<RsExpr> rest = exprs.size() > 1 ? exprs.subList(1, exprs.size()) : Collections.emptyList();
        finishWith(processCall(indexExpr, first, rest));
    }

    @Override
    public void visitUnaryExpr(@Nonnull RsUnaryExpr unaryExpr) {
        finishWith(processCall(unaryExpr, unaryExpr.getExpr(), Collections.emptyList()));
    }

    @Override
    public void visitTupleExpr(@Nonnull RsTupleExpr tupleExpr) {
        finishWith(straightLine(tupleExpr, getPred(), tupleExpr.getExprList()));
    }

    @Override
    public void visitStructLiteral(@Nonnull RsStructLiteral structLiteral) {
        RsStructLiteralBody body = structLiteral.getStructLiteralBody();
        List<RsStructLiteralField> fields = body.getStructLiteralFieldList();
        CFGInstruction fieldsExit = getPred();
        for (RsStructLiteralField field : fields) {
            fieldsExit = process(field, fieldsExit);
        }
        CFGInstruction exprExit = process(body.getExpr(), fieldsExit);
        finishWithAstNode(structLiteral, exprExit);
    }

    @Override
    public void visitStructLiteralField(@Nonnull RsStructLiteralField field) {
        CFGInstruction exprExit = process(field.getExpr(), getPred());
        finishWithAstNode(field, exprExit);
    }

    @Override
    public void visitCastExpr(@Nonnull RsCastExpr castExpr) {
        finishWith(straightLine(castExpr, getPred(), Collections.singletonList(castExpr.getExpr())));
    }

    @Override
    public void visitDotExpr(@Nonnull RsDotExpr dotExpr) {
        RsMethodCall methodCall = dotExpr.getMethodCall();
        if (methodCall == null) {
            finishWith(straightLine(dotExpr, getPred(), Collections.singletonList(dotExpr.getExpr())));
        } else {
            finishWith(processCall(dotExpr, dotExpr.getExpr(), methodCall.getValueArgumentList().getExprList()));
        }
    }

    @Override
    public void visitLitExpr(@Nonnull RsLitExpr litExpr) {
        finishWithAstNode(litExpr, getPred());
    }

    @Override
    public void visitUnitExpr(@Nonnull RsUnitExpr unitExpr) {
        finishWithAstNode(unitExpr, getPred());
    }

    @Override
    public void visitMatchExpr(@Nonnull RsMatchExpr matchExpr) {
        CFGInstruction discriminantExit = process(matchExpr.getExpr(), getPred());
        CFGInstruction exprExit = addAstNode(matchExpr);
        Deque<CFGInstruction> prevGuards = new ArrayDeque<>();

        List<RsMatchArm> arms = RsMatchExprUtil.getArms(matchExpr);
        if (arms.isEmpty()) {
            addContainedEdge(discriminantExit, exprExit);
        } else {
            for (RsMatchArm arm : arms) {
                CFGInstruction armExit = addDummyNode();
                RsMatchArmGuard guard = arm.getMatchArmGuard();

                for (RsPat pat : RsMatchArmUtil.getPatList(arm)) {
                    CFGInstruction patExit = process(pat, discriminantExit);
                    if (guard != null) {
                        CFGInstruction guardStart = addDummyNode(patExit);
                        CFGInstruction guardExit = process(guard, guardStart);
                        for (CFGInstruction pg : prevGuards) {
                            addContainedEdge(pg, guardStart);
                        }
                        prevGuards.clear();
                        prevGuards.push(guardExit);
                        patExit = guardExit;
                    }
                    addContainedEdge(patExit, armExit);
                }

                CFGInstruction bodyExit = process(arm.getExpr(), armExit);
                addContainedEdge(bodyExit, exprExit);
            }
        }

        finishWith(exprExit);
    }

    @Override
    public void visitMatchArmGuard(@Nonnull RsMatchArmGuard guard) {
        CFGInstruction conditionExit = process(guard.getExpr(), getPred());
        finishWithAstNode(guard, conditionExit);
    }

    @Override
    public void visitParenExpr(@Nonnull RsParenExpr parenExpr) {
        CFGInstruction exprExit = process(parenExpr.getExpr(), getPred());
        finishWithAstNode(parenExpr, exprExit);
    }

    @Override
    public void visitTryExpr(@Nonnull RsTryExpr tryExpr) {
        CFGInstruction tryExprExit = addAstNode(tryExpr);
        CFGInstruction exprExit = process(tryExpr.getExpr(), getPred());
        CFGInstruction checkExpr = addDummyNode(exprExit);
        addReturningEdge(checkExpr);
        addContainedEdge(exprExit, tryExprExit);
        finishWith(tryExprExit);
    }

    @Override
    public void visitLambdaExpr(@Nonnull RsLambdaExpr lambdaExpr) {
        CFGInstruction exprExit = process(lambdaExpr.getExpr(), getPred());
        CFGInstruction lambdaExprExit = addAstNode(lambdaExpr, exprExit);
        addContainedEdge(getPred(), lambdaExprExit);
        finishWith(lambdaExprExit);
    }

    @Override
    public void visitElement(@Nonnull RsElement element) {
        finishWith(getPred());
    }
}
