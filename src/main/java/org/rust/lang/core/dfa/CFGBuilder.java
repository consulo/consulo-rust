/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.dfa;

import com.intellij.psi.PsiElement;
import com.intellij.psi.util.PsiTreeUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.rust.lang.core.psi.*;
import org.rust.lang.core.psi.ext.*;
import org.rust.lang.core.types.regions.Scope;
import org.rust.lang.core.types.regions.ScopeTree;
import org.rust.lang.core.types.ty.TyNever;
import org.rust.lang.core.types.ty.TyPrimitive;
import org.rust.lang.utils.Graph;
import org.rust.lang.utils.Node;

import java.util.*;

public class CFGBuilder extends RsVisitor {

    public static class BlockScope {
        @NotNull public final RsBlockExpr blockExpr;
        @NotNull public final Node<CFGNodeData, CFGEdgeData> breakNode;

        public BlockScope(@NotNull RsBlockExpr blockExpr, @NotNull Node<CFGNodeData, CFGEdgeData> breakNode) {
            this.blockExpr = blockExpr;
            this.breakNode = breakNode;
        }
    }

    public static class LoopScope {
        @NotNull public final RsLooplikeExpr loop;
        @NotNull public final Node<CFGNodeData, CFGEdgeData> continueNode;
        @NotNull public final Node<CFGNodeData, CFGEdgeData> breakNode;

        public LoopScope(@NotNull RsLooplikeExpr loop, @NotNull Node<CFGNodeData, CFGEdgeData> continueNode, @NotNull Node<CFGNodeData, CFGEdgeData> breakNode) {
            this.loop = loop;
            this.continueNode = continueNode;
            this.breakNode = breakNode;
        }
    }

    public enum ScopeCFKind {
        Break, Continue;

        @NotNull
        public Node<CFGNodeData, CFGEdgeData> select(@NotNull Node<CFGNodeData, CFGEdgeData> breakNode, @NotNull Node<CFGNodeData, CFGEdgeData> continueNode) {
            return this == Break ? breakNode : continueNode;
        }
    }

    @NotNull private final ScopeTree regionScopeTree;
    @NotNull public final Graph<CFGNodeData, CFGEdgeData> graph;
    @NotNull public final Node<CFGNodeData, CFGEdgeData> entry;
    @NotNull public final Node<CFGNodeData, CFGEdgeData> exit;
    @NotNull private final Node<CFGNodeData, CFGEdgeData> termination;

    @Nullable private Node<CFGNodeData, CFGEdgeData> result;
    @NotNull private final Deque<Node<CFGNodeData, CFGEdgeData>> preds = new ArrayDeque<>();
    @NotNull private final Deque<LoopScope> loopScopes = new ArrayDeque<>();
    @NotNull private final Deque<BlockScope> breakableBlockScopes = new ArrayDeque<>();

    public CFGBuilder(
        @NotNull ScopeTree regionScopeTree,
        @NotNull Graph<CFGNodeData, CFGEdgeData> graph,
        @NotNull Node<CFGNodeData, CFGEdgeData> entry,
        @NotNull Node<CFGNodeData, CFGEdgeData> exit,
        @NotNull Node<CFGNodeData, CFGEdgeData> termination
    ) {
        this.regionScopeTree = regionScopeTree;
        this.graph = graph;
        this.entry = entry;
        this.exit = exit;
        this.termination = termination;
    }

    @NotNull
    private Node<CFGNodeData, CFGEdgeData> getPred() {
        return preds.peek();
    }

    private void finishWith(@NotNull Node<CFGNodeData, CFGEdgeData> value) {
        result = value;
    }

    private void finishWithAstNode(@NotNull RsElement element, @NotNull Node<CFGNodeData, CFGEdgeData>... preds) {
        result = addAstNode(element, preds);
    }

    private void finishWithUnreachableNode(@Nullable Node<CFGNodeData, CFGEdgeData> end) {
        if (end != null) {
            addTerminationEdge(end);
        }
        result = addUnreachableNode();
    }

    @NotNull
    private Node<CFGNodeData, CFGEdgeData> addAstNode(@NotNull RsElement element, @NotNull Node<CFGNodeData, CFGEdgeData>... preds) {
        return addNode(new CFGNodeData.AST(element), preds);
    }

    @NotNull
    private Node<CFGNodeData, CFGEdgeData> addDummyNode(@NotNull Node<CFGNodeData, CFGEdgeData>... preds) {
        return addNode(CFGNodeData.Dummy.INSTANCE, preds);
    }

    @NotNull
    private Node<CFGNodeData, CFGEdgeData> addUnreachableNode() {
        return addNode(CFGNodeData.Unreachable.INSTANCE);
    }

    @NotNull
    private Node<CFGNodeData, CFGEdgeData> addNode(@NotNull CFGNodeData data, @NotNull Node<CFGNodeData, CFGEdgeData>... preds) {
        Node<CFGNodeData, CFGEdgeData> newNode = graph.addNode(data);
        for (Node<CFGNodeData, CFGEdgeData> p : preds) {
            addContainedEdge(p, newNode);
        }
        return newNode;
    }

    public void addContainedEdge(@NotNull Node<CFGNodeData, CFGEdgeData> source, @NotNull Node<CFGNodeData, CFGEdgeData> target) {
        CFGEdgeData data = new CFGEdgeData(Collections.emptyList());
        graph.addEdge(source, target, data);
    }

    private void addReturningEdge(@NotNull Node<CFGNodeData, CFGEdgeData> fromNode) {
        List<RsElement> scopes = new ArrayList<>();
        for (LoopScope ls : loopScopes) {
            scopes.add(ls.loop);
        }
        CFGEdgeData data = new CFGEdgeData(scopes);
        graph.addEdge(fromNode, exit, data);
    }

    private void addTerminationEdge(@NotNull Node<CFGNodeData, CFGEdgeData> fromNode) {
        List<RsElement> scopes = new ArrayList<>();
        for (LoopScope ls : loopScopes) {
            scopes.add(ls.loop);
        }
        CFGEdgeData data = new CFGEdgeData(scopes);
        graph.addEdge(fromNode, termination, data);
    }

    private void addExitingEdge(@NotNull RsExpr fromExpr, @NotNull Node<CFGNodeData, CFGEdgeData> fromNode,
                                @NotNull Scope targetScope, @NotNull Node<CFGNodeData, CFGEdgeData> toNode) {
        List<RsElement> exitingScopes = new ArrayList<>();
        Scope scope = new Scope.Node(fromExpr);
        while (!scope.equals(targetScope)) {
            exitingScopes.add(scope.getElement());
            Scope enclosing = regionScopeTree.getEnclosingScope(scope);
            if (enclosing == null) break;
            scope = enclosing;
        }
        graph.addEdge(fromNode, toNode, new CFGEdgeData(exitingScopes));
    }

    @Nullable
    private Object[] findScopeEdge(@NotNull RsExpr expr, @Nullable RsLabel label, @NotNull ScopeCFKind kind) {
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
                    Node<CFGNodeData, CFGEdgeData> node = kind.select(ls.breakNode, ls.continueNode);
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
                    Node<CFGNodeData, CFGEdgeData> node = kind.select(ls.breakNode, ls.continueNode);
                    return new Object[]{new Scope.Node(ls.loop), node};
                }
            }
        }
        return null;
    }

    @NotNull
    private Node<CFGNodeData, CFGEdgeData> straightLine(@NotNull RsExpr expr, @NotNull Node<CFGNodeData, CFGEdgeData> pred, @NotNull List<RsExpr> subExprs) {
        Node<CFGNodeData, CFGEdgeData> subExprsExit = pred;
        for (RsExpr subExpr : subExprs) {
            subExprsExit = process(subExpr, subExprsExit);
        }
        return addAstNode(expr, subExprsExit);
    }

    @NotNull
    public Node<CFGNodeData, CFGEdgeData> process(@Nullable RsElement element, @NotNull Node<CFGNodeData, CFGEdgeData> pred) {
        if (element == null) return pred;

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

    @NotNull
    private Node<CFGNodeData, CFGEdgeData> processSubPats(@NotNull RsPat pat, @NotNull List<? extends RsPat> subPats) {
        Node<CFGNodeData, CFGEdgeData> patsExit = getPred();
        for (RsPat subPat : subPats) {
            patsExit = process(subPat, patsExit);
        }
        return addAstNode(pat, patsExit);
    }

    @NotNull
    private Node<CFGNodeData, CFGEdgeData> processConditionPats(@NotNull RsLetExpr letExpr, @NotNull Node<CFGNodeData, CFGEdgeData> pred) {
        List<RsPat> pats = RsLetExprUtil.getPatList(letExpr);
        if (pats == null) return pred;
        Node<CFGNodeData, CFGEdgeData> conditionExit = addDummyNode();
        for (RsPat pat : pats) {
            Node<CFGNodeData, CFGEdgeData> patExit = process(pat, pred);
            addContainedEdge(patExit, conditionExit);
        }
        return conditionExit;
    }

    @NotNull
    private Node<CFGNodeData, CFGEdgeData> processCall(@NotNull RsExpr callExpr, @Nullable RsExpr funcOrReceiver, @NotNull List<? extends RsExpr> args) {
        Node<CFGNodeData, CFGEdgeData> funcOrReceiverExit = process(funcOrReceiver, getPred());
        List<RsExpr> argsCopy = new ArrayList<>(args);
        Node<CFGNodeData, CFGEdgeData> callExit = straightLine(callExpr, funcOrReceiverExit, argsCopy);
        if (RsExprUtil.getType(callExpr) instanceof TyNever) {
            addTerminationEdge(callExit);
            return addUnreachableNode();
        }
        return callExit;
    }

    // Visitor methods

    @Override
    public void visitBlock(@NotNull RsBlock block) {
        RsBlockUtil.ExpandedStmtsAndTailExpr expanded = RsBlockUtil.getExpandedStmtsAndTailExpr(block);
        Node<CFGNodeData, CFGEdgeData> stmtsExit = getPred();
        for (RsElement stmt : expanded.getStmts()) {
            stmtsExit = process(stmt, stmtsExit);
        }
        RsExpr tailExpr = expanded.getTailExpr();
        if (tailExpr != null) {
            Node<CFGNodeData, CFGEdgeData> exprExit = process(tailExpr, stmtsExit);
            finishWithAstNode(block, exprExit);
        } else {
            finishWithAstNode(block, stmtsExit);
        }
    }

    @Override
    public void visitLetDecl(@NotNull RsLetDecl letDecl) {
        Node<CFGNodeData, CFGEdgeData> initExit = process(letDecl.getExpr(), getPred());
        Node<CFGNodeData, CFGEdgeData> exitNode = process(letDecl.getPat(), initExit);

        RsLetElseBranch elseBranch = letDecl.getLetElseBranch();
        if (elseBranch != null) {
            Node<CFGNodeData, CFGEdgeData> elseBranchExit = process(elseBranch.getBlock(), initExit);
            finishWithAstNode(letDecl, exitNode, elseBranchExit);
        } else {
            finishWithAstNode(letDecl, exitNode);
        }
    }

    @Override
    public void visitLetExpr(@NotNull RsLetExpr letExpr) {
        Node<CFGNodeData, CFGEdgeData> initExit = process(letExpr.getExpr(), getPred());
        Node<CFGNodeData, CFGEdgeData> exitNode = process(letExpr.getPat(), initExit);
        finishWithAstNode(letExpr, exitNode);
    }

    @Override
    public void visitNamedFieldDecl(@NotNull RsNamedFieldDecl fieldDecl) {
        finishWith(getPred());
    }

    @Override
    public void visitLabelDecl(@NotNull RsLabelDecl labelDecl) {
        finishWith(getPred());
    }

    @Override
    public void visitExprStmt(@NotNull RsExprStmt exprStmt) {
        Node<CFGNodeData, CFGEdgeData> exprExit = process(exprStmt.getExpr(), getPred());
        finishWithAstNode(exprStmt, exprExit);
    }

    @Override
    public void visitPatIdent(@NotNull RsPatIdent patIdent) {
        Node<CFGNodeData, CFGEdgeData> subPatExit = process(patIdent.getPat(), getPred());
        Node<CFGNodeData, CFGEdgeData> bindingExit = process(patIdent.getPatBinding(), subPatExit);
        finishWithAstNode(patIdent, bindingExit);
    }

    @Override
    public void visitPatBinding(@NotNull RsPatBinding patBinding) {
        finishWithAstNode(patBinding, getPred());
    }

    @Override
    public void visitPatRange(@NotNull RsPatRange patRange) {
        finishWithAstNode(patRange, getPred());
    }

    @Override
    public void visitPatConst(@NotNull RsPatConst patConst) {
        finishWithAstNode(patConst, getPred());
    }

    @Override
    public void visitPatWild(@NotNull RsPatWild patWild) {
        finishWithAstNode(patWild, getPred());
    }

    @Override
    public void visitPathExpr(@NotNull RsPathExpr pathExpr) {
        finishWithAstNode(pathExpr, getPred());
    }

    @Override
    public void visitMacroBodyIdent(@NotNull RsMacroBodyIdent macroBodyIdent) {
        finishWithAstNode(macroBodyIdent, getPred());
    }

    @Override
    public void visitMacroExpr(@NotNull RsMacroExpr macroExpr) {
        Node<CFGNodeData, CFGEdgeData> macroCallExit = process(macroExpr.getMacroCall(), getPred());
        if (macroCallExit != getPred() && RsExprUtil.getType(macroExpr) instanceof TyNever) {
            finishWithUnreachableNode(macroCallExit);
        } else {
            finishWith(macroCallExit);
        }
    }

    @Override
    public void visitMacroCall(@NotNull RsMacroCall macroCall) {
        Node<CFGNodeData, CFGEdgeData> subElementsExit;
        Object argument = RsMacroCallUtil.getMacroArgumentElement(macroCall);

        Node<CFGNodeData, CFGEdgeData> subExprsExit = null;
        if (argument instanceof RsExprMacroArgument) {
            RsExpr expr = ((RsExprMacroArgument) argument).getExpr();
            if (expr != null) subExprsExit = process(expr, getPred());
        } else if (argument instanceof RsIncludeMacroArgument) {
            RsExpr expr = ((RsIncludeMacroArgument) argument).getExpr();
            if (expr != null) subExprsExit = process(expr, getPred());
        } else if (argument instanceof RsConcatMacroArgument) {
            Node<CFGNodeData, CFGEdgeData> acc = getPred();
            for (RsExpr e : ((RsConcatMacroArgument) argument).getExprList()) acc = process(e, acc);
            subExprsExit = acc;
        } else if (argument instanceof RsEnvMacroArgument) {
            Node<CFGNodeData, CFGEdgeData> acc = getPred();
            for (RsExpr e : ((RsEnvMacroArgument) argument).getExprList()) acc = process(e, acc);
            subExprsExit = acc;
        } else if (argument instanceof RsVecMacroArgument) {
            Node<CFGNodeData, CFGEdgeData> acc = getPred();
            for (RsExpr e : ((RsVecMacroArgument) argument).getExprList()) acc = process(e, acc);
            subExprsExit = acc;
        } else if (argument instanceof RsFormatMacroArgument) {
            Object expansion = RsMacroCallUtil.getExpansion(macroCall);
            if (expansion != null) {
                Node<CFGNodeData, CFGEdgeData> acc = getPred();
                for (RsElement el : ((org.rust.lang.core.macros.MacroExpansion) expansion).getElements()) acc = process(el, acc);
                subExprsExit = acc;
            } else {
                Node<CFGNodeData, CFGEdgeData> acc = getPred();
                for (RsFormatMacroArg arg : ((RsFormatMacroArgument) argument).getFormatMacroArgList()) {
                    acc = process(arg.getExpr(), acc);
                }
                subExprsExit = acc;
            }
        } else if (argument instanceof RsAssertMacroArgument) {
            Node<CFGNodeData, CFGEdgeData> acc = getPred();
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
                Node<CFGNodeData, CFGEdgeData> acc = getPred();
                for (RsElement el : ((org.rust.lang.core.macros.MacroExpansion) expansion).getElements()) acc = process(el, acc);
                subExprsExit = acc;
            }
        }

        if (subExprsExit == null) {
            Collection<RsElement> subPathsIdents = PsiTreeUtil.findChildrenOfAnyType(
                macroCall, true, RsPathExpr.class, RsMacroBodyIdent.class
            );
            Node<CFGNodeData, CFGEdgeData> acc = getPred();
            for (RsElement el : subPathsIdents) acc = process(el, acc);
            subElementsExit = acc;
        } else {
            subElementsExit = subExprsExit;
        }

        finishWithAstNode(macroCall, subElementsExit);
    }

    @Override
    public void visitRangeExpr(@NotNull RsRangeExpr rangeExpr) {
        finishWith(straightLine(rangeExpr, getPred(), rangeExpr.getExprList()));
    }

    @Override
    public void visitPatTup(@NotNull RsPatTup patTup) {
        finishWith(processSubPats(patTup, patTup.getPatList()));
    }

    @Override
    public void visitPatTupleStruct(@NotNull RsPatTupleStruct patTupleStruct) {
        finishWith(processSubPats(patTupleStruct, patTupleStruct.getPatList()));
    }

    @Override
    public void visitPatStruct(@NotNull RsPatStruct patStruct) {
        Node<CFGNodeData, CFGEdgeData> patFieldsExit = getPred();
        for (RsPatField patField : patStruct.getPatFieldList()) {
            patFieldsExit = process(patField, patFieldsExit);
        }
        finishWithAstNode(patStruct, patFieldsExit);
    }

    @Override
    public void visitPatField(@NotNull RsPatField patField) {
        RsPatFieldFull patFieldFull = patField.getPatFieldFull();
        Node<CFGNodeData, CFGEdgeData> subPatExit;
        if (patFieldFull != null) {
            subPatExit = process(patFieldFull.getPat(), getPred());
        } else {
            subPatExit = process(patField.getPatBinding(), getPred());
        }
        finishWithAstNode(patField, subPatExit);
    }

    @Override
    public void visitPatSlice(@NotNull RsPatSlice patSlice) {
        finishWith(processSubPats(patSlice, patSlice.getPatList()));
    }

    @Override
    public void visitBlockExpr(@NotNull RsBlockExpr blockExpr) {
        RsLabelDecl labelDeclaration = blockExpr.getLabelDecl();
        boolean isAsync = RsBlockExprUtil.isAsync(blockExpr);
        Node<CFGNodeData, CFGEdgeData> exprExit = addAstNode(blockExpr);

        if (labelDeclaration != null) {
            breakableBlockScopes.push(new BlockScope(blockExpr, exprExit));
            Node<CFGNodeData, CFGEdgeData> stmtsExit = getPred();
            for (RsStmt stmt : blockExpr.getBlock().getStmtList()) {
                stmtsExit = process(stmt, stmtsExit);
            }
            Node<CFGNodeData, CFGEdgeData> blockExprExit = process(RsBlockUtil.getExpandedTailExpr(blockExpr.getBlock()), stmtsExit);
            addContainedEdge(blockExprExit, exprExit);
            breakableBlockScopes.pop();
        } else {
            Node<CFGNodeData, CFGEdgeData> blockExit = process(blockExpr.getBlock(), getPred());
            addContainedEdge(blockExit, exprExit);
        }

        if (isAsync) {
            addContainedEdge(getPred(), exprExit);
        }
        finishWith(exprExit);
    }

    @Override
    public void visitIfExpr(@NotNull RsIfExpr ifExpr) {
        RsCondition condition = ifExpr.getCondition();
        RsExpr expr = condition != null ? condition.getExpr() : null;

        Node<CFGNodeData, CFGEdgeData> exprExit;
        Node<CFGNodeData, CFGEdgeData> conditionPatsExit;
        if (expr instanceof RsLetExpr) {
            RsLetExpr letExpr = (RsLetExpr) expr;
            exprExit = process(letExpr.getExpr(), getPred());
            conditionPatsExit = processConditionPats(letExpr, exprExit);
        } else {
            exprExit = process(expr, getPred());
            conditionPatsExit = exprExit;
        }

        Node<CFGNodeData, CFGEdgeData> thenExit = process(ifExpr.getBlock(), conditionPatsExit);
        RsElseBranch elseBranch = ifExpr.getElseBranch();

        if (elseBranch != null) {
            RsBlock elseBranchBlock = elseBranch.getBlock();
            if (elseBranchBlock != null) {
                Node<CFGNodeData, CFGEdgeData> elseExit = process(elseBranchBlock, exprExit);
                finishWithAstNode(ifExpr, thenExit, elseExit);
            } else {
                Node<CFGNodeData, CFGEdgeData> nestedIfExit = process(elseBranch.getIfExpr(), exprExit);
                finishWithAstNode(ifExpr, thenExit, nestedIfExit);
            }
        } else {
            finishWithAstNode(ifExpr, exprExit, thenExit);
        }
    }

    @Override
    public void visitWhileExpr(@NotNull RsWhileExpr whileExpr) {
        Node<CFGNodeData, CFGEdgeData> loopBack = addDummyNode(getPred());
        Node<CFGNodeData, CFGEdgeData> whileExprExit = addAstNode(whileExpr);
        LoopScope loopScope = new LoopScope(whileExpr, loopBack, whileExprExit);

        loopScopes.push(loopScope);
        RsCondition condition = whileExpr.getCondition();
        RsExpr expr = condition != null ? condition.getExpr() : null;

        Node<CFGNodeData, CFGEdgeData> exprExit;
        if (expr instanceof RsLetExpr) {
            RsLetExpr letExpr = (RsLetExpr) expr;
            exprExit = process(letExpr.getExpr(), loopBack);
            addContainedEdge(exprExit, whileExprExit);
            exprExit = processConditionPats(letExpr, exprExit);
        } else {
            exprExit = process(expr, loopBack);
            addContainedEdge(exprExit, whileExprExit);
        }

        Node<CFGNodeData, CFGEdgeData> bodyExit = process(whileExpr.getBlock(), exprExit);
        addContainedEdge(bodyExit, loopBack);
        loopScopes.pop();

        finishWith(whileExprExit);
    }

    @Override
    public void visitLoopExpr(@NotNull RsLoopExpr loopExpr) {
        Node<CFGNodeData, CFGEdgeData> loopBack = addDummyNode(getPred());
        Node<CFGNodeData, CFGEdgeData> exprExit = addAstNode(loopExpr);
        LoopScope loopScope = new LoopScope(loopExpr, loopBack, exprExit);

        loopScopes.push(loopScope);
        Node<CFGNodeData, CFGEdgeData> bodyExit = process(loopExpr.getBlock(), loopBack);
        addContainedEdge(bodyExit, loopBack);
        loopScopes.pop();

        addTerminationEdge(loopBack);
        finishWith(exprExit);
    }

    @Override
    public void visitForExpr(@NotNull RsForExpr forExpr) {
        Node<CFGNodeData, CFGEdgeData> exprExit = addAstNode(forExpr);
        Node<CFGNodeData, CFGEdgeData> iterExprExit = process(forExpr.getExpr(), getPred());
        Node<CFGNodeData, CFGEdgeData> loopBack = addDummyNode(iterExprExit);
        addContainedEdge(loopBack, exprExit);

        LoopScope loopScope = new LoopScope(forExpr, loopBack, exprExit);
        loopScopes.push(loopScope);
        Node<CFGNodeData, CFGEdgeData> patExit = process(forExpr.getPat(), loopBack);
        Node<CFGNodeData, CFGEdgeData> bodyExit = process(forExpr.getBlock(), patExit);
        addContainedEdge(bodyExit, loopBack);
        loopScopes.pop();

        finishWith(exprExit);
    }

    @Override
    public void visitBinaryExpr(@NotNull RsBinaryExpr binaryExpr) {
        if (RsBinaryOpUtil.getOperatorType(binaryExpr.getBinaryOp()) instanceof LogicOp) {
            Node<CFGNodeData, CFGEdgeData> leftExit = process(binaryExpr.getLeft(), getPred());
            Node<CFGNodeData, CFGEdgeData> rightExit = process(binaryExpr.getRight(), leftExit);
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
    public void visitRetExpr(@NotNull RsRetExpr retExpr) {
        Node<CFGNodeData, CFGEdgeData> valueExit = process(retExpr.getExpr(), getPred());
        Node<CFGNodeData, CFGEdgeData> returnExit = addAstNode(retExpr, valueExit);
        addReturningEdge(returnExit);
        finishWithUnreachableNode(null);
    }

    @Override
    public void visitBreakExpr(@NotNull RsBreakExpr breakExpr) {
        Node<CFGNodeData, CFGEdgeData> exprExit = process(breakExpr.getExpr(), getPred());
        Object[] scopeEdge = findScopeEdge(breakExpr, breakExpr.getLabel(), ScopeCFKind.Break);
        if (scopeEdge == null) {
            finishWithUnreachableNode(exprExit);
            return;
        }
        Scope targetScope = (Scope) scopeEdge[0];
        Node<CFGNodeData, CFGEdgeData> breakDestination = (Node<CFGNodeData, CFGEdgeData>) scopeEdge[1];
        Node<CFGNodeData, CFGEdgeData> breakExit = addAstNode(breakExpr, exprExit);
        addExitingEdge(breakExpr, breakExit, targetScope, breakDestination);
        finishWithUnreachableNode(breakExit);
    }

    @Override
    public void visitContExpr(@NotNull RsContExpr contExpr) {
        Node<CFGNodeData, CFGEdgeData> contExit = addAstNode(contExpr, getPred());
        Object[] scopeEdge = findScopeEdge(contExpr, contExpr.getLabel(), ScopeCFKind.Continue);
        if (scopeEdge == null) {
            finishWithUnreachableNode(contExit);
            return;
        }
        Scope targetScope = (Scope) scopeEdge[0];
        Node<CFGNodeData, CFGEdgeData> contDestination = (Node<CFGNodeData, CFGEdgeData>) scopeEdge[1];
        addExitingEdge(contExpr, contExit, targetScope, contDestination);
        finishWithUnreachableNode(contExit);
    }

    @Override
    public void visitArrayExpr(@NotNull RsArrayExpr arrayExpr) {
        finishWith(straightLine(arrayExpr, getPred(), arrayExpr.getExprList()));
    }

    @Override
    public void visitCallExpr(@NotNull RsCallExpr callExpr) {
        finishWith(processCall(callExpr, callExpr.getExpr(), callExpr.getValueArgumentList().getExprList()));
    }

    @Override
    public void visitIndexExpr(@NotNull RsIndexExpr indexExpr) {
        List<RsExpr> exprs = indexExpr.getExprList();
        RsExpr first = exprs.isEmpty() ? null : exprs.get(0);
        List<RsExpr> rest = exprs.size() > 1 ? exprs.subList(1, exprs.size()) : Collections.emptyList();
        finishWith(processCall(indexExpr, first, rest));
    }

    @Override
    public void visitUnaryExpr(@NotNull RsUnaryExpr unaryExpr) {
        finishWith(processCall(unaryExpr, unaryExpr.getExpr(), Collections.emptyList()));
    }

    @Override
    public void visitTupleExpr(@NotNull RsTupleExpr tupleExpr) {
        finishWith(straightLine(tupleExpr, getPred(), tupleExpr.getExprList()));
    }

    @Override
    public void visitStructLiteral(@NotNull RsStructLiteral structLiteral) {
        RsStructLiteralBody body = structLiteral.getStructLiteralBody();
        List<RsStructLiteralField> fields = body.getStructLiteralFieldList();
        Node<CFGNodeData, CFGEdgeData> fieldsExit = getPred();
        for (RsStructLiteralField field : fields) {
            fieldsExit = process(field, fieldsExit);
        }
        Node<CFGNodeData, CFGEdgeData> exprExit = process(body.getExpr(), fieldsExit);
        finishWithAstNode(structLiteral, exprExit);
    }

    @Override
    public void visitStructLiteralField(@NotNull RsStructLiteralField field) {
        Node<CFGNodeData, CFGEdgeData> exprExit = process(field.getExpr(), getPred());
        finishWithAstNode(field, exprExit);
    }

    @Override
    public void visitCastExpr(@NotNull RsCastExpr castExpr) {
        finishWith(straightLine(castExpr, getPred(), Collections.singletonList(castExpr.getExpr())));
    }

    @Override
    public void visitDotExpr(@NotNull RsDotExpr dotExpr) {
        RsMethodCall methodCall = dotExpr.getMethodCall();
        if (methodCall == null) {
            finishWith(straightLine(dotExpr, getPred(), Collections.singletonList(dotExpr.getExpr())));
        } else {
            finishWith(processCall(dotExpr, dotExpr.getExpr(), methodCall.getValueArgumentList().getExprList()));
        }
    }

    @Override
    public void visitLitExpr(@NotNull RsLitExpr litExpr) {
        finishWithAstNode(litExpr, getPred());
    }

    @Override
    public void visitUnitExpr(@NotNull RsUnitExpr unitExpr) {
        finishWithAstNode(unitExpr, getPred());
    }

    @Override
    public void visitMatchExpr(@NotNull RsMatchExpr matchExpr) {
        Node<CFGNodeData, CFGEdgeData> discriminantExit = process(matchExpr.getExpr(), getPred());
        Node<CFGNodeData, CFGEdgeData> exprExit = addAstNode(matchExpr);
        Deque<Node<CFGNodeData, CFGEdgeData>> prevGuards = new ArrayDeque<>();

        List<RsMatchArm> arms = RsMatchExprUtil.getArms(matchExpr);
        if (arms.isEmpty()) {
            addContainedEdge(discriminantExit, exprExit);
        } else {
            for (RsMatchArm arm : arms) {
                Node<CFGNodeData, CFGEdgeData> armExit = addDummyNode();
                RsMatchArmGuard guard = arm.getMatchArmGuard();

                for (RsPat pat : RsMatchArmUtil.getPatList(arm)) {
                    Node<CFGNodeData, CFGEdgeData> patExit = process(pat, discriminantExit);
                    if (guard != null) {
                        Node<CFGNodeData, CFGEdgeData> guardStart = addDummyNode(patExit);
                        Node<CFGNodeData, CFGEdgeData> guardExit = process(guard, guardStart);
                        for (Node<CFGNodeData, CFGEdgeData> pg : prevGuards) {
                            addContainedEdge(pg, guardStart);
                        }
                        prevGuards.clear();
                        prevGuards.push(guardExit);
                        patExit = guardExit;
                    }
                    addContainedEdge(patExit, armExit);
                }

                Node<CFGNodeData, CFGEdgeData> bodyExit = process(arm.getExpr(), armExit);
                addContainedEdge(bodyExit, exprExit);
            }
        }

        finishWith(exprExit);
    }

    @Override
    public void visitMatchArmGuard(@NotNull RsMatchArmGuard guard) {
        Node<CFGNodeData, CFGEdgeData> conditionExit = process(guard.getExpr(), getPred());
        finishWithAstNode(guard, conditionExit);
    }

    @Override
    public void visitParenExpr(@NotNull RsParenExpr parenExpr) {
        Node<CFGNodeData, CFGEdgeData> exprExit = process(parenExpr.getExpr(), getPred());
        finishWithAstNode(parenExpr, exprExit);
    }

    @Override
    public void visitTryExpr(@NotNull RsTryExpr tryExpr) {
        Node<CFGNodeData, CFGEdgeData> tryExprExit = addAstNode(tryExpr);
        Node<CFGNodeData, CFGEdgeData> exprExit = process(tryExpr.getExpr(), getPred());
        Node<CFGNodeData, CFGEdgeData> checkExpr = addDummyNode(exprExit);
        addReturningEdge(checkExpr);
        addContainedEdge(exprExit, tryExprExit);
        finishWith(tryExprExit);
    }

    @Override
    public void visitLambdaExpr(@NotNull RsLambdaExpr lambdaExpr) {
        Node<CFGNodeData, CFGEdgeData> exprExit = process(lambdaExpr.getExpr(), getPred());
        Node<CFGNodeData, CFGEdgeData> lambdaExprExit = addAstNode(lambdaExpr, exprExit);
        addContainedEdge(getPred(), lambdaExprExit);
        finishWith(lambdaExprExit);
    }

    @Override
    public void visitElement(@NotNull RsElement element) {
        finishWith(getPred());
    }
}
