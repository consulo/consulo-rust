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
import consulo.language.controlFlow.ControlFlow;
import consulo.language.controlFlow.Instruction;

import java.util.*;
import org.rust.lang.core.psi.ext.impl.RsBlockUtil;
import org.rust.lang.core.psi.ext.impl.RsExprUtil;
import org.rust.lang.core.psi.ext.impl.RsElementExtUtil;

/**
 * The control-flow graph we use is built on the top of the PSI tree, as a platform {@link ControlFlow} of
 * {@link CFGInstruction}s: the entry, the normal exit and the termination (panics, infinite loops) come first.
 */
public class ControlFlowGraph implements ControlFlow {
    @Nonnull
    public final RsElement owner;
    @Nonnull
    public final RsBlock body;
    @Nonnull
    public final ScopeTree regionScopeTree;
    @Nonnull
    public final CFGInstruction entry;
    @Nonnull
    public final CFGInstruction exit;
    @Nonnull
    public final CFGInstruction termination;
    @Nonnull
    public final Set<RsElement> unreachableElements;

    @Nonnull
    private final CFGInstruction[] instructions;
    @Nonnull
    private final boolean[] reachable;
    @Nonnull
    private final Map<Long, List<RsElement>> exitingScopes;
    @Nonnull
    private final Map<RsElement, List<CFGInstruction>> localIndex;

    private ControlFlowGraph(
        @Nonnull RsElement owner,
        @Nonnull RsBlock body,
        @Nonnull ScopeTree regionScopeTree,
        @Nonnull CFGBuilder builder
    ) {
        this.owner = owner;
        this.body = body;
        this.regionScopeTree = regionScopeTree;
        this.entry = builder.entry;
        this.exit = builder.exit;
        this.termination = builder.termination;

        Instruction[] completed = builder.complete().getInstructions();
        this.instructions = new CFGInstruction[completed.length];
        for (int i = 0; i < completed.length; i++) {
            instructions[i] = (CFGInstruction) completed[i];
        }
        this.exitingScopes = builder.getExitingScopes();
        this.reachable = collectReachable(instructions, entry);
        this.unreachableElements = collectUnreachableElements(instructions, reachable);
        this.localIndex = buildLocalIndex(body, instructions, entry);
    }

    @Nonnull
    public static ControlFlowGraph buildFor(@Nonnull RsBlock body, @Nonnull ScopeTree regionScopeTree) {
        RsElement owner = (RsElement) body.getParent();
        CFGBuilder cfgBuilder = new CFGBuilder(regionScopeTree);
        CFGInstruction bodyExit = cfgBuilder.process(body, cfgBuilder.entry);
        cfgBuilder.addContainedEdge(bodyExit, cfgBuilder.exit);
        cfgBuilder.addContainedEdge(cfgBuilder.exit, cfgBuilder.termination);
        return new ControlFlowGraph(owner, body, regionScopeTree, cfgBuilder);
    }

    /** The key of the edge {@code source -> target} in {@link #getExitingScopes} */
    public static long edgeKey(@Nonnull Instruction source, @Nonnull Instruction target) {
        return ((long) source.num() << 32) | (target.num() & 0xFFFFFFFFL);
    }

    @Nonnull
    @Override
    public CFGInstruction[] getInstructions() {
        return instructions;
    }

    /** Whether the instruction is reachable from the {@link #entry} */
    public boolean isReachable(@Nonnull Instruction instruction) {
        return reachable[instruction.num()];
    }

    /** The scopes (loops, blocks) the edge {@code source -> target} exits */
    @Nonnull
    public List<RsElement> getExitingScopes(@Nonnull Instruction source, @Nonnull Instruction target) {
        return exitingScopes.getOrDefault(edgeKey(source, target), Collections.emptyList());
    }

    /**
     * The instructions of every element, the patterns of the function parameters mapped to the {@link #entry}.
     */
    @Nonnull
    public Map<RsElement, List<CFGInstruction>> getLocalIndex() {
        return localIndex;
    }

    @Nonnull
    private static boolean[] collectReachable(@Nonnull CFGInstruction[] instructions, @Nonnull CFGInstruction entry) {
        boolean[] reachable = new boolean[instructions.length];
        Deque<Instruction> stack = new ArrayDeque<>();
        stack.push(entry);
        reachable[entry.num()] = true;
        while (!stack.isEmpty()) {
            for (Instruction succ : stack.pop().allSucc()) {
                if (!reachable[succ.num()]) {
                    reachable[succ.num()] = true;
                    stack.push(succ);
                }
            }
        }
        return reachable;
    }

    @Nonnull
    private static Set<RsElement> collectUnreachableElements(@Nonnull CFGInstruction[] instructions, @Nonnull boolean[] reachable) {
        // Collect all unexecuted elements
        Set<RsElement> unexecutedElements = new HashSet<>();
        for (CFGInstruction instruction : instructions) {
            if (!reachable[instruction.num()]) {
                RsElement element = instruction.getElement();
                if (element != null) {
                    unexecutedElements.add(element);
                }
            }
        }

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
    private static Map<RsElement, List<CFGInstruction>> buildLocalIndex(
        @Nonnull RsBlock body,
        @Nonnull CFGInstruction[] instructions,
        @Nonnull CFGInstruction entry
    ) {
        Map<RsElement, List<CFGInstruction>> table = new HashMap<>();

        if (body.getParent() instanceof RsFunction rsFunc) {
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

        for (CFGInstruction instruction : instructions) {
            RsElement element = instruction.getElement();
            if (element != null) {
                table.computeIfAbsent(element, k -> new ArrayList<>()).add(instruction);
            }
        }
        return table;
    }
}
