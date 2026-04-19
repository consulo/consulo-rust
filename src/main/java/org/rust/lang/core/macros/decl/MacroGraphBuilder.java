/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.macros.decl;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.rust.lang.core.psi.ext.RsMacroDefinitionBase;
import org.rust.lang.utils.Node;
import org.rust.lang.utils.PresentableGraph;

import java.util.ArrayDeque;
import java.util.Deque;

/**
 * Builds a {@link PresentableGraph} (MacroGraph) from a macro definition for use in
 * completion and analysis of macro call bodies.
 */
public class MacroGraphBuilder {

    @NotNull
    private final RsMacroDefinitionBase myMacro;
    @NotNull
    private final PresentableGraph<MGNodeData, Void> myGraph;
    @NotNull
    private final Deque<Node<MGNodeData, Void>> myPreds;
    @Nullable
    private Node<MGNodeData, Void> myResult;

    public MacroGraphBuilder(@NotNull RsMacroDefinitionBase macro) {
        myMacro = macro;
        myGraph = new PresentableGraph<>();
        myPreds = new ArrayDeque<>();
        myResult = null;
    }

    @Nullable
    public PresentableGraph<MGNodeData, Void> build() {
        Matcher matcher = Matcher.buildFor(myMacro);
        if (matcher == null) return null;
        Node<MGNodeData, Void> start = addNode(MGNodeData.Start.INSTANCE);
        Node<MGNodeData, Void> exit = process(matcher, start);
        addNode(MGNodeData.End.INSTANCE, exit);
        return myGraph;
    }

    @NotNull
    private Node<MGNodeData, Void> process(@NotNull Matcher matcher, @NotNull Node<MGNodeData, Void> pred) {
        myResult = null;
        int oldSize = myPreds.size();
        myPreds.push(pred);
        processMatcher(matcher);
        myPreds.pop();
        assert myPreds.size() == oldSize;
        if (myResult == null) throw new IllegalStateException("Processing ended inconclusively");
        return myResult;
    }

    @NotNull
    private Node<MGNodeData, Void> addNode(@NotNull MGNodeData nodeData, @NotNull Node<MGNodeData, Void>... preds) {
        Node<MGNodeData, Void> newNode = myGraph.addNode(nodeData);
        for (Node<MGNodeData, Void> p : preds) {
            myGraph.addEdge(p, newNode, null);
        }
        return newNode;
    }

    private void addEdge(@NotNull Node<MGNodeData, Void> source, @NotNull Node<MGNodeData, Void> target) {
        myGraph.addEdge(source, target, null);
    }

    private void processMatcher(@NotNull Matcher matcher) {
        Node<MGNodeData, Void> pred = myPreds.peek();
        if (matcher instanceof Matcher.Fragment) {
            myResult = addNode(new MGNodeData.Fragment(((Matcher.Fragment) matcher).getKind()), pred);
        } else if (matcher instanceof Matcher.Literal) {
            myResult = addNode(new MGNodeData.Literal(((Matcher.Literal) matcher).getValue()), pred);
        } else if (matcher instanceof Matcher.Sequence) {
            Node<MGNodeData, Void> current = pred;
            for (Matcher sub : ((Matcher.Sequence) matcher).getMatchers()) {
                current = process(sub, current);
            }
            myResult = current;
        } else if (matcher instanceof Matcher.Choice) {
            Node<MGNodeData, Void> branchStart = addNode(MGNodeData.BranchStart.INSTANCE, pred);
            @SuppressWarnings("unchecked")
            Node<MGNodeData, Void>[] exits = ((Matcher.Choice) matcher).getMatchers().stream()
                .map(m -> process(m, branchStart))
                .toArray(Node[]::new);
            myResult = addNode(MGNodeData.BranchEnd.INSTANCE, exits);
        } else if (matcher instanceof Matcher.Optional) {
            Node<MGNodeData, Void> branchStart = addNode(MGNodeData.BranchStart.INSTANCE, pred);
            Node<MGNodeData, Void> optExit = process(((Matcher.Optional) matcher).getMatcher(), branchStart);
            Node<MGNodeData, Void> branchEnd = addNode(MGNodeData.BranchEnd.INSTANCE, optExit);
            addEdge(branchStart, branchEnd);
            myResult = branchEnd;
        } else if (matcher instanceof Matcher.Repeat) {
            Matcher.Repeat repeat = (Matcher.Repeat) matcher;
            Node<MGNodeData, Void> branchEnd = addNode(MGNodeData.BranchEnd.INSTANCE, pred);
            Node<MGNodeData, Void> current = branchEnd;
            for (Matcher sub : repeat.getMatchers()) {
                current = process(sub, current);
            }
            Node<MGNodeData, Void> branchStart = addNode(MGNodeData.BranchStart.INSTANCE, current);
            if (repeat.getSeparator() != null) {
                Node<MGNodeData, Void> separatorNode = addNode(new MGNodeData.Literal(repeat.getSeparator()), branchStart);
                addEdge(separatorNode, branchEnd);
            } else {
                addEdge(branchStart, branchEnd);
            }
            myResult = branchStart;
        }
    }
}
