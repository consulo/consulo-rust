/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.mir.dataflow.framework;

import org.jetbrains.annotations.NotNull;
import org.rust.lang.core.mir.schemas.*;

import java.util.BitSet;
import java.util.List;

public class BorrowCheckResults implements ResultsVisitable<BorrowCheckResults.State> {
    @NotNull
    private final Results<BitSet> uninits;
    @NotNull
    private final Results<BitSet> borrows;
    // TODO: EverInitializedPlaces

    public BorrowCheckResults(@NotNull Results<BitSet> uninits, @NotNull Results<BitSet> borrows) {
        this.uninits = uninits;
        this.borrows = borrows;
    }

    @Override
    @NotNull
    public Direction getDirection() {
        return Forward.INSTANCE;
    }

    @Override
    @NotNull
    public State getCopyOfBlockState(@NotNull MirBasicBlock block) {
        return new State(uninits.getCopyOfBlockState(block), borrows.getCopyOfBlockState(block));
    }

    @Override
    public void reconstructBeforeStatementEffect(@NotNull State state, @NotNull MirStatement statement, @NotNull MirLocation location) {
        uninits.reconstructBeforeStatementEffect(state.getUninits(), statement, location);
        borrows.reconstructBeforeStatementEffect(state.getBorrows(), statement, location);
    }

    @Override
    public void reconstructStatementEffect(@NotNull State state, @NotNull MirStatement statement, @NotNull MirLocation location) {
        uninits.reconstructStatementEffect(state.getUninits(), statement, location);
        borrows.reconstructStatementEffect(state.getBorrows(), statement, location);
    }

    @Override
    public void reconstructBeforeTerminatorEffect(@NotNull State state, @NotNull MirTerminator<MirBasicBlock> terminator, @NotNull MirLocation location) {
        uninits.reconstructBeforeTerminatorEffect(state.getUninits(), terminator, location);
        borrows.reconstructBeforeTerminatorEffect(state.getBorrows(), terminator, location);
    }

    @Override
    public void reconstructTerminatorEffect(@NotNull State state, @NotNull MirTerminator<MirBasicBlock> terminator, @NotNull MirLocation location) {
        uninits.reconstructTerminatorEffect(state.getUninits(), terminator, location);
        borrows.reconstructTerminatorEffect(state.getBorrows(), terminator, location);
    }

    /** Calls the corresponding method in ResultsVisitor for every location in a MirBody with the dataflow state at that location. */
    public static void visitResults(
        @NotNull BorrowCheckResults results,
        @NotNull List<MirBasicBlock> blocks,
        @NotNull ResultsVisitor<State> visitor
    ) {
        for (MirBasicBlock block : blocks) {
            results.getDirection().visitResultsInBlock(block, results, visitor);
        }
    }

    public static class State {
        @NotNull
        private final BitSet uninits;
        @NotNull
        private final BitSet borrows;

        public State(@NotNull BitSet uninits, @NotNull BitSet borrows) {
            this.uninits = uninits;
            this.borrows = borrows;
        }

        @NotNull
        public BitSet getUninits() {
            return uninits;
        }

        @NotNull
        public BitSet getBorrows() {
            return borrows;
        }
    }
}
