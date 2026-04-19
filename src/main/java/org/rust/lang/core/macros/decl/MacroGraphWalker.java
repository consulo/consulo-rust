/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.macros.decl;

import com.intellij.lang.PsiBuilder;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.TextRange;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.rust.lang.core.parser.ParserUtil;
import org.rust.lang.utils.Node;
import org.rust.lang.utils.PresentableGraph;

import java.util.*;

/**
 * Walks a {@link PresentableGraph} (MacroGraph) along with a macro call body and determines
 * possible {@link FragmentKind}s for a given caret offset.
 */
public class MacroGraphWalker {

    private enum Status { Active, Dead, Finished }

    @NotNull
    private final PsiBuilder myBuilder;
    @NotNull
    private final PresentableGraph<MGNodeData, Void> myGraph;
    @NotNull
    private final String myCallBody;
    private final int myCaretOffset;

    @NotNull
    private final Deque<WalkerState> myProcessStack;
    @Nullable
    private Node<MGNodeData, Void> myPosition;
    @NotNull
    private Status myStatus;

    @Nullable
    private FragmentDescriptor myDescriptor;
    @NotNull
    private final List<FragmentDescriptor> myResult;

    public MacroGraphWalker(
        @NotNull Project project,
        @NotNull PresentableGraph<MGNodeData, Void> graph,
        @NotNull String callBody,
        int caretOffset
    ) {
        myGraph = graph;
        myCallBody = callBody;
        myCaretOffset = caretOffset;
        myBuilder = ParserUtil.createAdaptedRustPsiBuilder(project, callBody);
        myBuilder.eof(); // skip whitespace
        myProcessStack = new ArrayDeque<>();
        myPosition = graph.getNode(0);
        myStatus = Status.Active;
        myDescriptor = null;
        myResult = new ArrayList<>();
    }

    @NotNull
    public List<FragmentDescriptor> run() {
        myProcessStack.push(new WalkerState(myPosition, myBuilder.mark(), myDescriptor));

        while (!myProcessStack.isEmpty()) {
            myStatus = Status.Active;
            WalkerState state = myProcessStack.pop();
            rollbackToState(state);

            processMatcher();

            switch (myStatus) {
                case Active: {
                    List<Node<MGNodeData, Void>> nextNodes = new ArrayList<>();
                    for (var edge : myGraph.outgoingEdges(myPosition)) {
                        nextNodes.add(edge.getTarget());
                    }

                    if (nextNodes.size() == 1) {
                        myProcessStack.push(new WalkerState(nextNodes.get(0), null, myDescriptor));
                    } else {
                        for (Node<MGNodeData, Void> node : nextNodes) {
                            myProcessStack.push(new WalkerState(node, myBuilder.mark(), myDescriptor));
                        }
                    }
                    break;
                }
                case Dead:
                    myDescriptor = null;
                    break;
                case Finished:
                    if (myDescriptor != null) {
                        myResult.add(myDescriptor);
                    }
                    break;
            }
        }

        return myResult;
    }

    private void rollbackToState(@NotNull WalkerState state) {
        myPosition = state.position;
        if (state.marker != null) {
            state.marker.rollbackTo();
        }
        myDescriptor = state.descriptor;
    }

    private void processMatcher() {
        MGNodeData data = myPosition.getData();
        if (data instanceof MGNodeData.Literal) {
            if (!DeclMacroExpander.isSameToken(myBuilder, ((MGNodeData.Literal) data).getValue())) {
                myStatus = Status.Dead;
            }
        } else if (data instanceof MGNodeData.Fragment) {
            FragmentKind kind = ((MGNodeData.Fragment) data).getKind();
            int fragmentStart = myBuilder.getCurrentOffset();
            if (kind.parse(myBuilder)) {
                if (myDescriptor == null) {
                    TextRange textRange = new TextRange(fragmentStart, myBuilder.getCurrentOffset() + 1);
                    if (textRange.contains(myCaretOffset) || myBuilder.eof()) {
                        int fragmentEnd = Math.min(myBuilder.getCurrentOffset(), myCallBody.length());
                        String fragmentText = myCallBody.substring(fragmentStart, fragmentEnd);
                        int caretOffsetInFragment = myCaretOffset - fragmentStart;
                        myDescriptor = new FragmentDescriptor(fragmentText, caretOffsetInFragment, kind);
                    }
                }
            } else {
                myStatus = Status.Dead;
            }
        } else if (data instanceof MGNodeData.End) {
            myStatus = myBuilder.eof() ? Status.Finished : Status.Dead;
        }
        // Start, BranchStart, BranchEnd - do nothing

        if (myStatus == Status.Active && myBuilder.eof()) {
            myStatus = Status.Finished;
        }
    }

    // --- Inner classes ---

    public static final class FragmentDescriptor {
        @NotNull
        private final String myFragmentText;
        private final int myCaretOffsetInFragment;
        @NotNull
        private final FragmentKind myKind;

        public FragmentDescriptor(@NotNull String fragmentText, int caretOffsetInFragment, @NotNull FragmentKind kind) {
            myFragmentText = fragmentText;
            myCaretOffsetInFragment = caretOffsetInFragment;
            myKind = kind;
        }

        @NotNull
        public String getFragmentText() {
            return myFragmentText;
        }

        public int getCaretOffsetInFragment() {
            return myCaretOffsetInFragment;
        }

        @NotNull
        public FragmentKind getKind() {
            return myKind;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof FragmentDescriptor)) return false;
            FragmentDescriptor that = (FragmentDescriptor) o;
            return myCaretOffsetInFragment == that.myCaretOffsetInFragment
                && Objects.equals(myFragmentText, that.myFragmentText)
                && myKind == that.myKind;
        }

        @Override
        public int hashCode() {
            return Objects.hash(myFragmentText, myCaretOffsetInFragment, myKind);
        }

        @Override
        public String toString() {
            return "FragmentDescriptor(fragmentText=" + myFragmentText +
                ", caretOffset=" + myCaretOffsetInFragment +
                ", kind=" + myKind + ")";
        }
    }

    private static final class WalkerState {
        @Nullable
        final Node<MGNodeData, Void> position;
        @Nullable
        final PsiBuilder.Marker marker;
        @Nullable
        final FragmentDescriptor descriptor;

        WalkerState(
            @Nullable Node<MGNodeData, Void> position,
            @Nullable PsiBuilder.Marker marker,
            @Nullable FragmentDescriptor descriptor
        ) {
            this.position = position;
            this.marker = marker;
            this.descriptor = descriptor;
        }
    }
}
