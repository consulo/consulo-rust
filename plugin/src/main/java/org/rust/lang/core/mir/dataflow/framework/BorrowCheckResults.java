/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.mir.dataflow.framework;

import jakarta.annotation.Nonnull;
import org.rust.lang.core.mir.schemas.*;

import java.util.BitSet;
import java.util.List;

public class BorrowCheckResults implements ResultsVisitable<BorrowCheckResults.State> {
    @Nonnull
    private final Results<BitSet> uninits;
    @Nonnull
    private final Results<BitSet> borrows;
    // TODO: EverInitializedPlaces

    public BorrowCheckResults(@Nonnull Results<BitSet> uninits, @Nonnull Results<BitSet> borrows) {
        this.uninits = uninits;
        this.borrows = borrows;
    }

    @Override
    @Nonnull
    public Direction getDirection() {
        return Forward.INSTANCE;
    }

    @Override
    @Nonnull
    public State getCopyOfBlockState(@Nonnull MirBasicBlock block) {
        return new State(uninits.getCopyOfBlockState(block), borrows.getCopyOfBlockState(block));
    }

    @Override
    public void reconstructBeforeStatementEffect(@Nonnull State state, @Nonnull MirStatement statement, @Nonnull MirLocation location) {
        uninits.reconstructBeforeStatementEffect(state.getUninits(), statement, location);
        borrows.reconstructBeforeStatementEffect(state.getBorrows(), statement, location);
    }

    @Override
    public void reconstructStatementEffect(@Nonnull State state, @Nonnull MirStatement statement, @Nonnull MirLocation location) {
        uninits.reconstructStatementEffect(state.getUninits(), statement, location);
        borrows.reconstructStatementEffect(state.getBorrows(), statement, location);
    }

    @Override
    public void reconstructBeforeTerminatorEffect(@Nonnull State state, @Nonnull MirTerminator<MirBasicBlock> terminator, @Nonnull MirLocation location) {
        uninits.reconstructBeforeTerminatorEffect(state.getUninits(), terminator, location);
        borrows.reconstructBeforeTerminatorEffect(state.getBorrows(), terminator, location);
    }

    @Override
    public void reconstructTerminatorEffect(@Nonnull State state, @Nonnull MirTerminator<MirBasicBlock> terminator, @Nonnull MirLocation location) {
        uninits.reconstructTerminatorEffect(state.getUninits(), terminator, location);
        borrows.reconstructTerminatorEffect(state.getBorrows(), terminator, location);
    }

    /** Calls the corresponding method in ResultsVisitor for every location in a MirBody with the dataflow state at that location. */
    public static void visitResults(
        @Nonnull BorrowCheckResults results,
        @Nonnull List<MirBasicBlock> blocks,
        @Nonnull ResultsVisitor<State> visitor
    ) {
        for (MirBasicBlock block : blocks) {
            results.getDirection().visitResultsInBlock(block, results, visitor);
        }
    }

    public static class State {
        @Nonnull
        private final BitSet uninits;
        @Nonnull
        private final BitSet borrows;

        public State(@Nonnull BitSet uninits, @Nonnull BitSet borrows) {
            this.uninits = uninits;
            this.borrows = borrows;
        }

        @Nonnull
        public BitSet getUninits() {
            return uninits;
        }

        @Nonnull
        public BitSet getBorrows() {
            return borrows;
        }
    }
}
