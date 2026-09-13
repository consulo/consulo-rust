/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.dfa;


import org.rust.lang.core.psi.ext.impl.RsElementUtil;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.rust.lang.core.psi.*;
import org.rust.lang.core.psi.ext.RsElement;
import org.rust.lang.core.types.regions.ScopeTree;
import org.rust.lang.core.types.ty.TyNever;
import org.rust.common.graph.Node;
import org.rust.common.graph.PresentableGraph;

import java.util.*;
import org.rust.lang.core.psi.ext.impl.RsBlockUtil;
import org.rust.lang.core.psi.ext.impl.RsExprUtil;
import org.rust.lang.core.psi.ext.impl.RsElementExtUtil;

/**
 * The control-flow graph we use is built on the top of the PSI tree.
 */
public class ControlFlowGraph {
    @Nonnull
    public final RsElement owner;
    @Nonnull
    public final PresentableGraph<CFGNodeData, CFGEdgeData> graph;
    @Nonnull
    public final RsBlock body;
    @Nonnull
    public final ScopeTree regionScopeTree;
    @Nonnull
    public final Node<CFGNodeData, CFGEdgeData> entry;
    @Nonnull
    public final Node<CFGNodeData, CFGEdgeData> exit;
    @Nonnull
    public final Set<RsElement> unreachableElements;

    private ControlFlowGraph(
        @Nonnull RsElement owner,
        @Nonnull PresentableGraph<CFGNodeData, CFGEdgeData> graph,
        @Nonnull RsBlock body,
        @Nonnull ScopeTree regionScopeTree,
        @Nonnull Node<CFGNodeData, CFGEdgeData> entry,
        @Nonnull Node<CFGNodeData, CFGEdgeData> exit,
        @Nonnull Set<RsElement> unreachableElements
    ) {
        this.owner = owner;
        this.graph = graph;
        this.body = body;
        this.regionScopeTree = regionScopeTree;
        this.entry = entry;
        this.exit = exit;
        this.unreachableElements = unreachableElements;
    }

    @Nonnull
    public static ControlFlowGraph buildFor(@Nonnull RsBlock body, @Nonnull ScopeTree regionScopeTree) {
        RsElement owner = (RsElement) body.getParent();
        PresentableGraph<CFGNodeData, CFGEdgeData> graph = new PresentableGraph<>();
        Node<CFGNodeData, CFGEdgeData> entry = graph.addNode(CFGNodeData.Entry.INSTANCE);
        Node<CFGNodeData, CFGEdgeData> fnExit = graph.addNode(CFGNodeData.Exit.INSTANCE);
        Node<CFGNodeData, CFGEdgeData> termination = graph.addNode(CFGNodeData.Termination.INSTANCE);

        CFGBuilder cfgBuilder = new CFGBuilder(regionScopeTree, graph, entry, fnExit, termination);
        Node<CFGNodeData, CFGEdgeData> bodyExit = cfgBuilder.process(body, entry);
        cfgBuilder.addContainedEdge(bodyExit, fnExit);
        cfgBuilder.addContainedEdge(fnExit, termination);

        Set<RsElement> unreachableElements = collectUnreachableElements(graph, entry);

        return new ControlFlowGraph(owner, graph, body, regionScopeTree, entry, fnExit, unreachableElements);
    }

    @Nonnull
    private static Set<RsElement> collectUnreachableElements(@Nonnull PresentableGraph<CFGNodeData, CFGEdgeData> graph, @Nonnull Node<CFGNodeData, CFGEdgeData> entry) {
        // Collect all unexecuted elements
        Set<RsElement> unexecutedElements = new HashSet<>();
        Set<Integer> fullyExecutedNodeIndices = new HashSet<>();
        for (Node<CFGNodeData, CFGEdgeData> node : graph.depthFirstTraversal(entry)) {
            fullyExecutedNodeIndices.add(node.getIndex());
        }
        graph.forEachNode(node -> {
            if (!fullyExecutedNodeIndices.contains(node.getIndex())) {
                RsElement element = node.getData().getElement();
                if (element != null) {
                    unexecutedElements.add(element);
                }
            }
        });

        List<RsStmt> unexecutedStmts = new ArrayList<>();
        for (RsElement el : unexecutedElements) {
            if (el instanceof RsStmt) unexecutedStmts.add((RsStmt) el);
        }

        List<RsExpr> unexecutedTailExprs = new ArrayList<>();
        for (RsElement el : unexecutedElements) {
            if (el instanceof RsBlock) {
                RsBlock block = (RsBlock) el;
                RsExpr tailExpr = RsBlockUtil.getExpandedTailExpr(block);
                if (tailExpr != null) {
                    boolean isUnexecuted;
                    if (tailExpr instanceof RsMacroExpr) {
                        isUnexecuted = unexecutedElements.contains(((RsMacroExpr) tailExpr).getMacroCall());
                    } else {
                        isUnexecuted = unexecutedElements.contains(tailExpr);
                    }
                    if (isUnexecuted) {
                        unexecutedTailExprs.add(tailExpr);
                    }
                }
            }
        }

        Set<RsElement> unreachableElements = new HashSet<>();

        for (RsStmt stmt : unexecutedStmts) {
            if (stmt instanceof RsExprStmt && isUnreachable(stmt, ((RsExprStmt) stmt).getExpr(), unexecutedElements)) {
                unreachableElements.add(stmt);
            } else if (stmt instanceof RsLetDecl && isUnreachable(stmt, ((RsLetDecl) stmt).getExpr(), unexecutedElements)) {
                unreachableElements.add(stmt);
            }
        }
        for (RsExpr tailExpr : unexecutedTailExprs) {
            if (isUnreachable(tailExpr, tailExpr, unexecutedElements)) {
                unreachableElements.add(tailExpr);
            }
        }

        return unreachableElements;
    }

    private static boolean isUnreachable(
        @Nonnull RsElement unexecuted,
        @Nullable RsExpr innerExpr,
        @Nonnull Set<RsElement> unexecutedElements
    ) {
        if (!RsElementExtUtil.getExistsAfterExpansion(unexecuted)) return false;
        if (innerExpr != null && !RsElementExtUtil.getExistsAfterExpansion(innerExpr)) return false;
        if (innerExpr != null && !(RsExprUtil.getType(innerExpr) instanceof TyNever)) return true;

        RsBlock parentBlock = RsElementUtil.ancestorStrict(unexecuted, RsBlock.class);
        if (parentBlock == null) return false;
        List<RsStmt> blockStmts = parentBlock.getStmtList();
        if (blockStmts.isEmpty()) return false;
        RsExpr blockTailExpr = RsBlockUtil.getExpandedTailExpr(parentBlock);
        int index = blockStmts.indexOf(unexecuted);
        if (index >= 1) {
            return unexecutedElements.contains(blockStmts.get(index - 1));
        } else if (unexecuted == blockTailExpr) {
            return unexecutedElements.contains(blockStmts.get(blockStmts.size() - 1));
        }
        return false;
    }

    @Nonnull
    public HashMap<RsElement, List<Node<CFGNodeData, CFGEdgeData>>> buildLocalIndex() {
        HashMap<RsElement, List<Node<CFGNodeData, CFGEdgeData>>> table = new HashMap<>();
        Object func = body.getParent();

        if (func instanceof RsFunction) {
            RsFunction rsFunc = (RsFunction) func;
            RsVisitor formals = new RsVisitor() {
                @Override
                public void visitPatBinding(@Nonnull RsPatBinding binding) {
                    table.computeIfAbsent(binding, k -> new ArrayList<>()).add(entry);
                }

                @Override
                public void visitPatField(@Nonnull RsPatField field) {
                    field.acceptChildren(this);
                }

                @Override
                public void visitPat(@Nonnull RsPat pat) {
                    pat.acceptChildren(this);
                }
            };

            for (RsValueParameter param : rsFunc.getValueParameters()) {
                RsPat pat = param.getPat();
                if (pat != null) {
                    formals.visitPat(pat);
                }
            }
        }

        graph.forEachNode(node -> {
            RsElement element = node.getData().getElement();
            if (element != null) {
                table.computeIfAbsent(element, k -> new ArrayList<>()).add(node);
            }
        });

        return table;
    }

    // typealias CFGNode = Node<CFGNodeData, CFGEdgeData>
    // typealias CFGGraph = PresentableGraph<CFGNodeData, CFGEdgeData>
}
