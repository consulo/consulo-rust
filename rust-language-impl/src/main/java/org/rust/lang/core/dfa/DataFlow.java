/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.dfa;


import jakarta.annotation.Nonnull;
import org.rust.lang.core.psi.ext.RsElement;
import consulo.language.controlFlow.Instruction;

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
        @Nonnull private final Map<RsElement, List<CFGInstruction>> cfgTable;

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

            int size = cfg.getInstructions().length * wordsPerElement;
            this.gens = new int[size];
            this.actionKills = new int[size];
            this.scopeKills = new int[size];
            this.onEntry = new int[size];
            Arrays.fill(this.onEntry, oper.getNeutralElement());
            this.cfgTable = cfg.getLocalIndex();
        }

        @Nonnull
        private List<CFGInstruction> getCfgNodes(@Nonnull RsElement element) {
            return cfgTable.getOrDefault(element, Collections.emptyList());
        }

        private boolean hasBitSetForElement(@Nonnull RsElement element) {
            return cfgTable.containsKey(element);
        }

        private int getStart(@Nonnull CFGInstruction node) {
            return node.num() * wordsPerElement;
        }

        private int getEnd(@Nonnull CFGInstruction node) {
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
            for (CFGInstruction node : getCfgNodes(element)) {
                setBit(gens, getStart(node), bit);
            }
        }

        public void addKill(@Nonnull KillFrom kind, @Nonnull RsElement element, int bit) {
            for (CFGInstruction node : getCfgNodes(element)) {
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
        private int[] applyGenKill(@Nonnull CFGInstruction node, @Nonnull int[] bits) {
            int start = getStart(node);
            int[] result = Arrays.copyOf(bits, bits.length);
            bitwiseRange(UNION, result, gens, start, wordsPerElement);
            bitwiseRange(SUBTRACT, result, actionKills, start, wordsPerElement);
            bitwiseRange(SUBTRACT, result, scopeKills, start, wordsPerElement);
            return result;
        }

        private static void bitwiseRange(@Nonnull BitwiseOperator op, @Nonnull int[] out, @Nonnull int[] in, int inOffset, int len) {
            bitwiseRange(op, out, 0, in, inOffset, len);
        }

        private static void bitwiseRange(@Nonnull BitwiseOperator op, @Nonnull int[] out, int outOffset, @Nonnull int[] in, int inOffset, int len) {
            for (int i = 0; i < len; i++) {
                out[outOffset + i] = op.join(out[outOffset + i], in[inOffset + i]);
            }
        }

        public boolean eachBitOnEntry(@Nonnull RsElement element, @Nonnull IntPredicate predicate) {
            if (!hasBitSetForElement(element)) return true;
            List<CFGInstruction> nodes = getCfgNodes(element);
            for (CFGInstruction node : nodes) {
                if (!eachBitForNode(EntryOrExit.Entry, node, predicate)) return false;
            }
            return true;
        }

        private boolean eachBitForNode(@Nonnull EntryOrExit e, @Nonnull CFGInstruction node, @Nonnull IntPredicate predicate) {
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
            for (CFGInstruction flowExit : cfg.getInstructions()) {
                for (Instruction target : flowExit.allSucc()) {
                    List<RsElement> scopes = cfg.getExitingScopes(flowExit, target);
                    if (scopes.isEmpty()) continue;
                    int start = getStart(flowExit);
                    for (RsElement element : scopes) {
                        List<CFGInstruction> cfgNodes = cfgTable.get(element);
                        if (cfgNodes == null) continue;
                        for (CFGInstruction cfgNode : cfgNodes) {
                            bitwiseRange(UNION, scopeKills, start, scopeKills, getStart(cfgNode), wordsPerElement);
                        }
                    }
                }
            }
        }

        public void propagate() {
            if (bitsPerElement == 0) return;

            List<CFGInstruction> orderedNodes = postOrder(cfg);
            if (flowDirection == FlowDirection.Forward) {
                Collections.reverse(orderedNodes);
            }

            boolean changed = true;
            while (changed) {
                changed = false;
                for (CFGInstruction node : orderedNodes) {
                    int start = getStart(node);
                    int[] nodeOnEntry = Arrays.copyOfRange(onEntry, start, start + wordsPerElement);
                    int[] result = applyGenKill(node, nodeOnEntry);
                    Collection<Instruction> next = flowDirection == FlowDirection.Forward ? node.allSucc() : node.allPred();
                    for (Instruction target : next) {
                        if (propagateBitsIntoEntrySetFor(result, (CFGInstruction) target)) changed = true;
                    }
                }
            }
        }

        /**
         * Every instruction, each after all its successors except along back edges: a depth-first walk from the entry,
         * then from each instruction not reached yet, so that unreachable code is analysed too
         */
        @Nonnull
        private static List<CFGInstruction> postOrder(@Nonnull ControlFlowGraph cfg) {
            CFGInstruction[] instructions = cfg.getInstructions();
            List<CFGInstruction> result = new ArrayList<>(instructions.length);
            boolean[] visited = new boolean[instructions.length];
            Deque<CFGInstruction> stack = new ArrayDeque<>();
            Deque<Iterator<Instruction>> successors = new ArrayDeque<>();
            List<CFGInstruction> roots = new ArrayList<>(instructions.length + 1);
            roots.add(cfg.entry);
            roots.addAll(Arrays.asList(instructions));
            for (CFGInstruction root : roots) {
                if (visited[root.num()]) continue;
                visited[root.num()] = true;
                stack.push(root);
                successors.push(root.allSucc().iterator());
                walk(stack, successors, visited, result);
            }
            return result;
        }

        private static void walk(
            @Nonnull Deque<CFGInstruction> stack,
            @Nonnull Deque<Iterator<Instruction>> successors,
            @Nonnull boolean[] visited,
            @Nonnull List<CFGInstruction> result
        ) {
            while (!stack.isEmpty()) {
                Iterator<Instruction> iterator = successors.peek();
                if (iterator.hasNext()) {
                    CFGInstruction succ = (CFGInstruction) iterator.next();
                    if (!visited[succ.num()]) {
                        visited[succ.num()] = true;
                        stack.push(succ);
                        successors.push(succ.allSucc().iterator());
                    }
                } else {
                    successors.pop();
                    result.add(stack.pop());
                }
            }
        }

        private boolean propagateBitsIntoEntrySetFor(@Nonnull int[] predBits, @Nonnull CFGInstruction node) {
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
