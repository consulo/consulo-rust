/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.types.infer;

import com.intellij.openapi.progress.ProgressManager;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.function.Function;

/**
 * ObligationForest is a mutable collection of obligations.
 */
public class ObligationForest {
    private final boolean myTraceObligations;

    public enum NodeState {
        Pending,
        Success,
        Error
    }

    public static class ProcessObligationsResult {
        private final boolean myHasErrors;
        private final boolean myStalled;

        public ProcessObligationsResult(boolean hasErrors, boolean stalled) {
            myHasErrors = hasErrors;
            myStalled = stalled;
        }

        public boolean isHasErrors() {
            return myHasErrors;
        }

        public boolean isStalled() {
            return myStalled;
        }
    }

    public static class Node {
        @NotNull
        private final PendingPredicateObligation myObligation;
        @NotNull
        private NodeState myState = NodeState.Pending;

        public Node(@NotNull PendingPredicateObligation obligation) {
            myObligation = obligation;
        }

        @NotNull
        public PendingPredicateObligation getObligation() {
            return myObligation;
        }

        @NotNull
        public NodeState getState() {
            return myState;
        }

        public void setState(@NotNull NodeState state) {
            myState = state;
        }
    }

    private final List<Node> myNodes = new ArrayList<>();
    private final Set<Predicate> myDoneCache = new HashSet<>();
    private final List<Node> myRoots = new ArrayList<>();
    private final Map<Node, List<Node>> myParentToChildren = new HashMap<>();

    public ObligationForest(boolean traceObligations) {
        myTraceObligations = traceObligations;
    }

    @NotNull
    public List<Node> getRoots() {
        return myRoots;
    }

    @NotNull
    public Map<Node, List<Node>> getParentToChildren() {
        return myParentToChildren;
    }

    @NotNull
    public Iterable<PendingPredicateObligation> getPendingObligations() {
        List<PendingPredicateObligation> result = new ArrayList<>();
        for (Node node : myNodes) {
            if (node.myState == NodeState.Pending) {
                result.add(node.myObligation);
            }
        }
        return result;
    }

    public void registerObligationAt(@NotNull PendingPredicateObligation obligation, @Nullable Node parent) {
        if (myDoneCache.add(obligation.getObligation().getPredicate())) {
            Node node = new Node(obligation);
            myNodes.add(node);
            if (myTraceObligations) {
                if (parent == null) {
                    myRoots.add(node);
                } else {
                    myParentToChildren.computeIfAbsent(parent, k -> new ArrayList<>()).add(node);
                }
            }
        }
    }

    @NotNull
    public ProcessObligationsResult processObligations(
        @NotNull Function<PendingPredicateObligation, ProcessPredicateResult> processor,
        boolean breakOnFirstError
    ) {
        boolean hasErrors = false;
        boolean stalled = true;
        int size = myNodes.size();
        for (int index = 0; index < size; index++) {
            ProgressManager.checkCanceled();
            Node node = myNodes.get(index);
            if (node.myState != NodeState.Pending) continue;

            ProcessPredicateResult result = processor.apply(node.myObligation);
            if (result == ProcessPredicateResult.NoChanges) {
                // do nothing
            } else if (result instanceof ProcessPredicateResult.Ok) {
                stalled = false;
                node.myState = NodeState.Success;
                for (PendingPredicateObligation child : ((ProcessPredicateResult.Ok) result).getChildren()) {
                    registerObligationAt(child, node);
                }
            } else if (result == ProcessPredicateResult.Err) {
                hasErrors = true;
                stalled = false;
                node.myState = NodeState.Error;
                if (breakOnFirstError) return new ProcessObligationsResult(true, false);
            }
        }

        if (!stalled) {
            myNodes.removeIf(n -> n.myState != NodeState.Pending);
        }

        return new ProcessObligationsResult(hasErrors, stalled);
    }

    @NotNull
    public ProcessObligationsResult processObligations(
        @NotNull Function<PendingPredicateObligation, ProcessPredicateResult> processor
    ) {
        return processObligations(processor, false);
    }
}
