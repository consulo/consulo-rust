/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.dfa;

import org.jetbrains.annotations.NotNull;
import org.rust.lang.core.psi.ext.RsElement;
import org.rust.lang.utils.Node;

import java.util.*;
import java.util.function.IntPredicate;

public class DataFlow {
    private DataFlow() {
    }

    public enum EntryOrExit { Entry, Exit }
    public enum FlowDirection { Forward, Backward }
    public enum KillFrom {
        ScopeEnd,   // e.g. a kill associated with the end of the scope of a variable declaration
        Execution   // e.g. a kill associated with an assignment statement
    }

    public interface BitwiseOperator {
        int join(int succ, int pred);

        default boolean bitwise(@NotNull int[] outBits, @NotNull int[] inBits) {
            boolean changed = false;
            for (int i = 0; i < outBits.length && i < inBits.length; i++) {
                int newValue = join(outBits[i], inBits[i]);
                if (outBits[i] != newValue) changed = true;
                outBits[i] = newValue;
            }
            return changed;
        }
    }

    public static final BitwiseOperator UNION = (succ, pred) -> succ | pred;
    public static final BitwiseOperator SUBTRACT = (succ, pred) -> succ & ~pred;

    public interface DataFlowOperator extends BitwiseOperator {
        boolean getInitialValue();

        default int getNeutralElement() {
            return getInitialValue() ? Integer.MAX_VALUE : 0;
        }
    }

    public static class DataFlowContext<O extends DataFlowOperator> {
        private static final int BITS_PER_INT = 32;

        @NotNull private final ControlFlowGraph cfg;
        @NotNull private final O oper;
        private final int bitsPerElement;
        @NotNull private final FlowDirection flowDirection;
        private final int wordsPerElement;
        @NotNull private final int[] gens;
        @NotNull private final int[] scopeKills;
        @NotNull private final int[] actionKills;
        @NotNull private final int[] onEntry;
        @NotNull private final Map<RsElement, List<Node<CFGNodeData, CFGEdgeData>>> cfgTable;

        public DataFlowContext(
            @NotNull ControlFlowGraph cfg,
            @NotNull O oper,
            int bitsPerElement,
            @NotNull FlowDirection flowDirection
        ) {
            this.cfg = cfg;
            this.oper = oper;
            this.bitsPerElement = bitsPerElement;
            this.flowDirection = flowDirection;
            this.wordsPerElement = (bitsPerElement + BITS_PER_INT - 1) / BITS_PER_INT;

            int size = cfg.graph.getNodesCount() * wordsPerElement;
            this.gens = new int[size];
            this.actionKills = new int[size];
            this.scopeKills = new int[size];
            this.onEntry = new int[size];
            Arrays.fill(this.onEntry, oper.getNeutralElement());
            this.cfgTable = cfg.buildLocalIndex();
        }

        @NotNull
        private List<Node<CFGNodeData, CFGEdgeData>> getCfgNodes(@NotNull RsElement element) {
            return cfgTable.getOrDefault(element, Collections.emptyList());
        }

        private boolean hasBitSetForElement(@NotNull RsElement element) {
            return cfgTable.containsKey(element);
        }

        private int getStart(@NotNull Node<CFGNodeData, CFGEdgeData> node) {
            return node.getIndex() * wordsPerElement;
        }

        private int getEnd(@NotNull Node<CFGNodeData, CFGEdgeData> node) {
            return getStart(node) + wordsPerElement;
        }

        private boolean setBit(@NotNull int[] words, int wordOffset, int bit) {
            int word = bit / BITS_PER_INT;
            int bitInWord = bit % BITS_PER_INT;
            int bitMask = 1 << bitInWord;
            int oldValue = words[wordOffset + word];
            int newValue = oldValue | bitMask;
            words[wordOffset + word] = newValue;
            return oldValue != newValue;
        }

        public void addGen(@NotNull RsElement element, int bit) {
            for (Node<CFGNodeData, CFGEdgeData> node : getCfgNodes(element)) {
                setBit(gens, getStart(node), bit);
            }
        }

        public void addKill(@NotNull KillFrom kind, @NotNull RsElement element, int bit) {
            for (Node<CFGNodeData, CFGEdgeData> node : getCfgNodes(element)) {
                int start = getStart(node);
                switch (kind) {
                    case ScopeEnd:
                        setBit(scopeKills, start, bit);
                        break;
                    case Execution:
                        setBit(actionKills, start, bit);
                        break;
                }
            }
        }

        @NotNull
        private int[] applyGenKill(@NotNull Node<CFGNodeData, CFGEdgeData> node, @NotNull int[] bits) {
            int start = getStart(node);
            int[] result = Arrays.copyOf(bits, bits.length);
            bitwiseRange(UNION, result, gens, start, wordsPerElement);
            bitwiseRange(SUBTRACT, result, actionKills, start, wordsPerElement);
            bitwiseRange(SUBTRACT, result, scopeKills, start, wordsPerElement);
            return result;
        }

        private static void bitwiseRange(@NotNull BitwiseOperator op, @NotNull int[] out, @NotNull int[] in, int inOffset, int len) {
            for (int i = 0; i < len; i++) {
                out[i] = op.join(out[i], in[inOffset + i]);
            }
        }

        public boolean eachBitOnEntry(@NotNull RsElement element, @NotNull IntPredicate predicate) {
            if (!hasBitSetForElement(element)) return true;
            List<Node<CFGNodeData, CFGEdgeData>> nodes = getCfgNodes(element);
            for (Node<CFGNodeData, CFGEdgeData> node : nodes) {
                if (!eachBitForNode(EntryOrExit.Entry, node, predicate)) return false;
            }
            return true;
        }

        private boolean eachBitForNode(@NotNull EntryOrExit e, @NotNull Node<CFGNodeData, CFGEdgeData> node, @NotNull IntPredicate predicate) {
            if (bitsPerElement == 0) return true;
            int start = getStart(node);
            int[] slice;
            if (e == EntryOrExit.Entry) {
                slice = Arrays.copyOfRange(onEntry, start, start + wordsPerElement);
            } else {
                slice = applyGenKill(node, Arrays.copyOfRange(onEntry, start, start + wordsPerElement));
            }
            return eachBit(slice, predicate);
        }

        private boolean eachBit(@NotNull int[] words, @NotNull IntPredicate predicate) {
            for (int index = 0; index < words.length; index++) {
                int word = words[index];
                if (word == 0) continue;
                int baseIndex = index * BITS_PER_INT;
                for (int offset = 0; offset < BITS_PER_INT; offset++) {
                    int bit = 1 << offset;
                    if ((word & bit) != 0) {
                        int bitIndex = baseIndex + offset;
                        if (bitIndex >= bitsPerElement) return true;
                        if (!predicate.test(bitIndex)) return false;
                    }
                }
            }
            return true;
        }

        public void addKillsFromFlowExits() {
            if (bitsPerElement == 0) return;
            cfg.graph.forEachEdge(edge -> {
                Node<CFGNodeData, CFGEdgeData> flowExit = edge.getSource();
                int start = getStart(flowExit);
                int[] originalKills = Arrays.copyOfRange(scopeKills, start, start + wordsPerElement);

                boolean changed = false;
                for (RsElement element : edge.getData().getExitingScopes()) {
                    List<Node<CFGNodeData, CFGEdgeData>> cfgNodes = cfgTable.get(element);
                    if (cfgNodes == null) continue;
                    for (Node<CFGNodeData, CFGEdgeData> cfgNode : cfgNodes) {
                        int nodeStart = getStart(cfgNode);
                        int[] kills = Arrays.copyOfRange(scopeKills, nodeStart, nodeStart + wordsPerElement);
                        int[] temp = new int[wordsPerElement];
                        System.arraycopy(originalKills, 0, temp, 0, wordsPerElement);
                        if (UNION.bitwise(temp, kills)) {
                            System.arraycopy(temp, 0, originalKills, 0, wordsPerElement);
                            changed = true;
                        }
                    }
                }
                if (changed) {
                    System.arraycopy(originalKills, 0, scopeKills, start, wordsPerElement);
                }
            });
        }

        public void propagate() {
            if (bitsPerElement == 0) return;

            List<Node<CFGNodeData, CFGEdgeData>> orderedNodes;
            List<Node<CFGNodeData, CFGEdgeData>> postOrder = cfg.graph.nodesInPostOrder(cfg.entry);
            if (flowDirection == FlowDirection.Forward) {
                orderedNodes = new ArrayList<>(postOrder);
                Collections.reverse(orderedNodes);
            } else {
                orderedNodes = postOrder;
            }

            boolean changed = true;
            while (changed) {
                changed = false;
                for (Node<CFGNodeData, CFGEdgeData> node : orderedNodes) {
                    int start = getStart(node);
                    int[] nodeOnEntry = Arrays.copyOfRange(onEntry, start, start + wordsPerElement);
                    int[] result = applyGenKill(node, nodeOnEntry);
                    if (flowDirection == FlowDirection.Forward) {
                        for (Object edge : cfg.graph.outgoingEdges(node)) {
                            Node<CFGNodeData, CFGEdgeData> target = ((org.rust.lang.utils.Edge<CFGNodeData, CFGEdgeData>) edge).getTarget();
                            if (propagateBitsIntoEntrySetFor(result, target)) changed = true;
                        }
                    } else {
                        for (Object edge : cfg.graph.incomingEdges(node)) {
                            Node<CFGNodeData, CFGEdgeData> source = ((org.rust.lang.utils.Edge<CFGNodeData, CFGEdgeData>) edge).getSource();
                            if (propagateBitsIntoEntrySetFor(result, source)) changed = true;
                        }
                    }
                }
            }
        }

        private boolean propagateBitsIntoEntrySetFor(@NotNull int[] predBits, @NotNull Node<CFGNodeData, CFGEdgeData> node) {
            int start = getStart(node);
            boolean changed = false;
            for (int i = 0; i < wordsPerElement; i++) {
                int newValue = oper.join(onEntry[start + i], predBits[i]);
                if (onEntry[start + i] != newValue) {
                    onEntry[start + i] = newValue;
                    changed = true;
                }
            }
            return changed;
        }
    }
}
