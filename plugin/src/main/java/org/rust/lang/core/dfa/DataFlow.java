/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.dfa;

import jakarta.annotation.Nonnull;
import org.rust.lang.core.psi.ext.RsElement;
import org.rust.lang.utils.Node;

import java.util.*;
import java.util.function.IntPredicate;
import org.rust.lang.utils.Edge;

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

        default boolean bitwise(@Nonnull int[] outBits, @Nonnull int[] inBits) {
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

        @Nonnull private final ControlFlowGraph cfg;
        @Nonnull private final O oper;
        private final int bitsPerElement;
        @Nonnull private final FlowDirection flowDirection;
        private final int wordsPerElement;
        @Nonnull private final int[] gens;
        @Nonnull private final int[] scopeKills;
        @Nonnull private final int[] actionKills;
        @Nonnull private final int[] onEntry;
        @Nonnull private final Map<RsElement, List<Node<CFGNodeData, CFGEdgeData>>> cfgTable;

        public DataFlowContext(
            @Nonnull ControlFlowGraph cfg,
            @Nonnull O oper,
            int bitsPerElement,
            @Nonnull FlowDirection flowDirection
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

        @Nonnull
        private List<Node<CFGNodeData, CFGEdgeData>> getCfgNodes(@Nonnull RsElement element) {
            return cfgTable.getOrDefault(element, Collections.emptyList());
        }

        private boolean hasBitSetForElement(@Nonnull RsElement element) {
            return cfgTable.containsKey(element);
        }

        private int getStart(@Nonnull Node<CFGNodeData, CFGEdgeData> node) {
            return node.getIndex() * wordsPerElement;
        }

        private int getEnd(@Nonnull Node<CFGNodeData, CFGEdgeData> node) {
            return getStart(node) + wordsPerElement;
        }

        private boolean setBit(@Nonnull int[] words, int wordOffset, int bit) {
            int word = bit / BITS_PER_INT;
            int bitInWord = bit % BITS_PER_INT;
            int bitMask = 1 << bitInWord;
            int oldValue = words[wordOffset + word];
            int newValue = oldValue | bitMask;
            words[wordOffset + word] = newValue;
            return oldValue != newValue;
        }

        public void addGen(@Nonnull RsElement element, int bit) {
            for (Node<CFGNodeData, CFGEdgeData> node : getCfgNodes(element)) {
                setBit(gens, getStart(node), bit);
            }
        }

        public void addKill(@Nonnull KillFrom kind, @Nonnull RsElement element, int bit) {
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

        @Nonnull
        private int[] applyGenKill(@Nonnull Node<CFGNodeData, CFGEdgeData> node, @Nonnull int[] bits) {
            int start = getStart(node);
            int[] result = Arrays.copyOf(bits, bits.length);
            bitwiseRange(UNION, result, gens, start, wordsPerElement);
            bitwiseRange(SUBTRACT, result, actionKills, start, wordsPerElement);
            bitwiseRange(SUBTRACT, result, scopeKills, start, wordsPerElement);
            return result;
        }

        private static void bitwiseRange(@Nonnull BitwiseOperator op, @Nonnull int[] out, @Nonnull int[] in, int inOffset, int len) {
            for (int i = 0; i < len; i++) {
                out[i] = op.join(out[i], in[inOffset + i]);
            }
        }

        public boolean eachBitOnEntry(@Nonnull RsElement element, @Nonnull IntPredicate predicate) {
            if (!hasBitSetForElement(element)) return true;
            List<Node<CFGNodeData, CFGEdgeData>> nodes = getCfgNodes(element);
            for (Node<CFGNodeData, CFGEdgeData> node : nodes) {
                if (!eachBitForNode(EntryOrExit.Entry, node, predicate)) return false;
            }
            return true;
        }

        private boolean eachBitForNode(@Nonnull EntryOrExit e, @Nonnull Node<CFGNodeData, CFGEdgeData> node, @Nonnull IntPredicate predicate) {
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

        private boolean eachBit(@Nonnull int[] words, @Nonnull IntPredicate predicate) {
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

        private boolean propagateBitsIntoEntrySetFor(@Nonnull int[] predBits, @Nonnull Node<CFGNodeData, CFGEdgeData> node) {
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
