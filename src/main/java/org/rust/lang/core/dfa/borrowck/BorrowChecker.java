/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.dfa.borrowck;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.rust.lang.core.dfa.ControlFlowGraph;
import org.rust.lang.core.dfa.MemoryCategorization.Cmt;
import org.rust.lang.core.dfa.borrowck.gatherLoans.GatherLoanContext;
import org.rust.lang.core.psi.RsBlock;
import org.rust.lang.core.psi.ext.RsElement;
import org.rust.lang.core.psi.ext.RsInferenceContextOwner;
import org.rust.lang.core.psi.ext.RsInferenceContextOwnerUtil;
import org.rust.lang.core.resolve.ImplLookup;
import org.rust.lang.core.types.ExtensionsUtil;
import org.rust.lang.core.types.infer.RsInferenceResult;

import java.util.*;

public class BorrowChecker {
    private BorrowChecker() {
    }

    // ---- BorrowCheckContext ----
    public static class BorrowCheckContext {
        @NotNull
        private final RsInferenceResult inference;
        @NotNull
        private final RsBlock body;
        @NotNull
        private final ControlFlowGraph cfg;
        @NotNull
        private final ImplLookup implLookup;
        @NotNull
        private final Set<UseOfMovedValueError> usesOfMovedValue;
        @NotNull
        private final Set<UseOfUninitializedVariable> usesOfUninitializedVariable;
        @NotNull
        private final Set<MoveError> moveErrors;

        private BorrowCheckContext(
            @NotNull RsInferenceResult inference,
            @NotNull RsBlock body,
            @NotNull ControlFlowGraph cfg,
            @NotNull ImplLookup implLookup
        ) {
            this.inference = inference;
            this.body = body;
            this.cfg = cfg;
            this.implLookup = implLookup;
            this.usesOfMovedValue = new HashSet<>();
            this.usesOfUninitializedVariable = new HashSet<>();
            this.moveErrors = new HashSet<>();
        }

        @Nullable
        public static BorrowCheckContext buildFor(@NotNull RsInferenceContextOwner owner) {
            Object bodyObj = RsInferenceContextOwnerUtil.getBody(owner);
            if (!(bodyObj instanceof RsBlock)) return null;
            RsBlock body = (RsBlock) bodyObj;
            ControlFlowGraph cfg = ExtensionsUtil.getControlFlowGraph(owner);
            if (cfg == null) return null;
            return new BorrowCheckContext(
                ExtensionsUtil.getSelfInferenceResult(owner),
                body,
                cfg,
                ImplLookup.relativeTo(body)
            );
        }

        @NotNull
        public RsInferenceResult getInference() {
            return inference;
        }

        @NotNull
        public RsBlock getBody() {
            return body;
        }

        @NotNull
        public ControlFlowGraph getCfg() {
            return cfg;
        }

        @NotNull
        public ImplLookup getImplLookup() {
            return implLookup;
        }

        @NotNull
        public BorrowCheckResult check() {
            AnalysisData data = buildAnalysisData(this);
            if (data != null) {
                CheckLoanContext clcx = new CheckLoanContext(this, data.moveData);
                clcx.checkLoans(body);
            }
            return new BorrowCheckResult(
                new ArrayList<>(usesOfMovedValue),
                new ArrayList<>(usesOfUninitializedVariable),
                new ArrayList<>(moveErrors)
            );
        }

        @Nullable
        private AnalysisData buildAnalysisData(@NotNull BorrowCheckContext bccx) {
            GatherLoanContext glcx = new GatherLoanContext(this);
            MoveData moveData = glcx.check();
            if (moveData.isEmpty()) return null;
            FlowedMoveData flowedMoves = FlowedMoveData.buildFor(moveData, bccx, cfg);
            return new AnalysisData(flowedMoves);
        }

        public void reportUseOfMovedValue(@NotNull LoanPath loanPath, @NotNull Move move) {
            if (move.kind == MoveKind.Declared) {
                usesOfUninitializedVariable.add(new UseOfUninitializedVariable(loanPath.element));
            } else {
                usesOfMovedValue.add(new UseOfMovedValueError(loanPath.element, move));
            }
        }

        public void reportMoveError(@NotNull Cmt from) {
            moveErrors.add(new MoveError(from));
        }
    }

    // ---- AnalysisData ----
    public static class AnalysisData {
        @NotNull
        public final FlowedMoveData moveData;

        public AnalysisData(@NotNull FlowedMoveData moveData) {
            this.moveData = moveData;
        }
    }

    // ---- BorrowCheckResult ----
    public static class BorrowCheckResult {
        @NotNull
        public final List<UseOfMovedValueError> usesOfMovedValue;
        @NotNull
        public final List<UseOfUninitializedVariable> usesOfUninitializedVariable;
        @NotNull
        public final List<MoveError> moveErrors;

        public BorrowCheckResult(
            @NotNull List<UseOfMovedValueError> usesOfMovedValue,
            @NotNull List<UseOfUninitializedVariable> usesOfUninitializedVariable,
            @NotNull List<MoveError> moveErrors
        ) {
            this.usesOfMovedValue = usesOfMovedValue;
            this.usesOfUninitializedVariable = usesOfUninitializedVariable;
            this.moveErrors = moveErrors;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof BorrowCheckResult)) return false;
            BorrowCheckResult that = (BorrowCheckResult) o;
            return usesOfMovedValue.equals(that.usesOfMovedValue) &&
                usesOfUninitializedVariable.equals(that.usesOfUninitializedVariable) &&
                moveErrors.equals(that.moveErrors);
        }

        @Override
        public int hashCode() {
            return Objects.hash(usesOfMovedValue, usesOfUninitializedVariable, moveErrors);
        }
    }

    // ---- UseOfMovedValueError ----
    public static class UseOfMovedValueError {
        @NotNull
        public final RsElement use;
        @NotNull
        public final Move move;

        public UseOfMovedValueError(@NotNull RsElement use, @NotNull Move move) {
            this.use = use;
            this.move = move;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof UseOfMovedValueError)) return false;
            UseOfMovedValueError that = (UseOfMovedValueError) o;
            return use.equals(that.use) && move.equals(that.move);
        }

        @Override
        public int hashCode() {
            return Objects.hash(use, move);
        }
    }

    // ---- UseOfUninitializedVariable ----
    public static class UseOfUninitializedVariable {
        @NotNull
        public final RsElement use;

        public UseOfUninitializedVariable(@NotNull RsElement use) {
            this.use = use;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof UseOfUninitializedVariable)) return false;
            return use.equals(((UseOfUninitializedVariable) o).use);
        }

        @Override
        public int hashCode() {
            return use.hashCode();
        }
    }

    // ---- MoveError ----
    public static class MoveError {
        @NotNull
        public final Cmt from;

        public MoveError(@NotNull Cmt from) {
            this.from = from;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof MoveError)) return false;
            return from.equals(((MoveError) o).from);
        }

        @Override
        public int hashCode() {
            return from.hashCode();
        }
    }
}
