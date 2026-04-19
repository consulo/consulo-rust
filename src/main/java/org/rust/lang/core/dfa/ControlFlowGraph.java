/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.dfa;

import org.rust.lang.core.psi.ext.RsElementUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.rust.lang.core.psi.*;
import org.rust.lang.core.psi.ext.RsElement;
import org.rust.lang.core.psi.ext.RsOuterAttributeOwnerUtil;
import org.rust.lang.core.types.regions.ScopeTree;
import org.rust.lang.core.types.ty.TyNever;
import org.rust.lang.utils.Node;
import org.rust.lang.utils.PresentableGraph;
import org.rust.lang.utils.PresentableNodeData;
import org.rust.stdext.StdextUtil;

import java.util.*;
import org.rust.lang.core.psi.ext.RsBlockUtil;
import org.rust.lang.core.psi.ext.RsExprUtil;
import org.rust.lang.core.psi.ext.RsElementExtUtil;

/**
 * The control-flow graph we use is built on the top of the PSI tree.
 */
public class ControlFlowGraph {
    @NotNull
    public final RsElement owner;
    @NotNull
    public final PresentableGraph<CFGNodeData, CFGEdgeData> graph;
    @NotNull
    public final RsBlock body;
    @NotNull
    public final ScopeTree regionScopeTree;
    @NotNull
    public final Node<CFGNodeData, CFGEdgeData> entry;
    @NotNull
    public final Node<CFGNodeData, CFGEdgeData> exit;
    @NotNull
    public final Set<RsElement> unreachableElements;

    private ControlFlowGraph(
        @NotNull RsElement owner,
        @NotNull PresentableGraph<CFGNodeData, CFGEdgeData> graph,
        @NotNull RsBlock body,
        @NotNull ScopeTree regionScopeTree,
        @NotNull Node<CFGNodeData, CFGEdgeData> entry,
        @NotNull Node<CFGNodeData, CFGEdgeData> exit,
        @NotNull Set<RsElement> unreachableElements
    ) {
        this.owner = owner;
        this.graph = graph;
        this.body = body;
        this.regionScopeTree = regionScopeTree;
        this.entry = entry;
        this.exit = exit;
        this.unreachableElements = unreachableElements;
    }

    @NotNull
    public static ControlFlowGraph buildFor(@NotNull RsBlock body, @NotNull ScopeTree regionScopeTree) {
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

    @NotNull
    private static Set<RsElement> collectUnreachableElements(@NotNull PresentableGraph<CFGNodeData, CFGEdgeData> graph, @NotNull Node<CFGNodeData, CFGEdgeData> entry) {
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
        @NotNull RsElement unexecuted,
        @Nullable RsExpr innerExpr,
        @NotNull Set<RsElement> unexecutedElements
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

    @NotNull
    public HashMap<RsElement, List<Node<CFGNodeData, CFGEdgeData>>> buildLocalIndex() {
        HashMap<RsElement, List<Node<CFGNodeData, CFGEdgeData>>> table = new HashMap<>();
        Object func = body.getParent();

        if (func instanceof RsFunction) {
            RsFunction rsFunc = (RsFunction) func;
            RsVisitor formals = new RsVisitor() {
                @Override
                public void visitPatBinding(@NotNull RsPatBinding binding) {
                    table.computeIfAbsent(binding, k -> new ArrayList<>()).add(entry);
                }

                @Override
                public void visitPatField(@NotNull RsPatField field) {
                    field.acceptChildren(this);
                }

                @Override
                public void visitPat(@NotNull RsPat pat) {
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
