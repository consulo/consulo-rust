/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.mir.dataflow.impls;

import org.jetbrains.annotations.NotNull;
import org.rust.lang.core.mir.dataflow.framework.*;
import org.rust.lang.core.mir.schemas.*;

import java.util.*;

public class Borrows implements GenKillAnalysis {
    @NotNull
    private final BorrowSet borrowSet;
    @NotNull
    private final Map<MirLocation, List<BorrowData>> borrowsOutOfScopeAtLocation;

    public Borrows(
        @NotNull BorrowSet borrowSet,
        @NotNull Map<MirLocation, List<BorrowData>> borrowsOutOfScopeAtLocation
    ) {
        this.borrowSet = borrowSet;
        this.borrowsOutOfScopeAtLocation = borrowsOutOfScopeAtLocation;
    }

    @Override
    @NotNull
    public Direction getDirection() {
        return Forward.INSTANCE;
    }

    // bottom = nothing is reserved or activated yet
    @Override
    @NotNull
    public BitSet bottomValue(@NotNull MirBody body) {
        return new BitSet(borrowSet.getSize());
    }

    @Override
    public void initializeStartBlock(@NotNull MirBody body, @NotNull BitSet state) {
        // no borrows of code region_scopes have been taken prior to function execution, so this method has no effect.
    }

    @Override
    public void applyBeforeStatementEffect(@NotNull BitSet state, @NotNull MirStatement statement, @NotNull MirLocation location) {
        killLoansOutOfScopeAtLocation(state, location);
    }

    @Override
    public void applyStatementEffect(@NotNull BitSet state, @NotNull MirStatement statement, @NotNull MirLocation location) {
        if (statement instanceof MirStatement.Assign) {
            MirStatement.Assign assign = (MirStatement.Assign) statement;
            MirRvalue rhs = assign.getRvalue();
            if (rhs instanceof MirRvalue.Ref) {
                MirRvalue.Ref ref = (MirRvalue.Ref) rhs;
                if (BorrowsUtil.ignoreBorrow(ref.getPlace(), borrowSet.getLocalsStateAtExit())) return;
                BorrowData borrowData = borrowSet.getLocationMap().get(location);
                if (borrowData == null) return;
                state.set(borrowData.getIndex(), true);
            }

            // Make sure there are no remaining borrows for variables that are assigned over.
            killBorrowsOnPlace(state, assign.getPlace());
        } else if (statement instanceof MirStatement.StorageDead) {
            // Make sure there are no remaining borrows for locals that are gone out of scope.
            killBorrowsOnPlace(state, new MirPlace(((MirStatement.StorageDead) statement).getLocal()));
        }
    }

    @Override
    public void applyBeforeTerminatorEffect(@NotNull BitSet state, @NotNull MirTerminator<MirBasicBlock> terminator, @NotNull MirLocation location) {
        killLoansOutOfScopeAtLocation(state, location);
    }

    @Override
    public void applyTerminatorEffect(@NotNull BitSet state, @NotNull MirTerminator<MirBasicBlock> terminator, @NotNull MirLocation location) {
        // TODO: process TerminatorKind::InlineAsm
    }

    /**
     * Add all borrows to the kill set, if those borrows are out of scope at the given location.
     * That means they went out of a nonlexical scope.
     */
    private void killLoansOutOfScopeAtLocation(@NotNull BitSet state, @NotNull MirLocation location) {
        // NOTE: The state associated with a given location reflects the dataflow on entry to the statement.
        // Iterate over each of the borrows that we've precomputed to have went out of scope at this location and kill
        // them.
        //
        // We are careful always to call this function *before* we set up the gen-bits for the statement or terminator.
        // That way, if the effect of the statement or terminator *does* introduce a new loan of the same region, then
        // setting that gen-bit will override any potential kill introduced here.
        List<BorrowData> borrows = borrowsOutOfScopeAtLocation.get(location);
        if (borrows == null) return;
        for (BorrowData borrow : borrows) {
            state.set(borrow.getIndex(), false);
        }
    }

    /** Kill any borrows that conflict with the given place. */
    private void killBorrowsOnPlace(@NotNull BitSet state, @NotNull MirPlace place) {
        Set<BorrowData> otherBorrowsOfLocal = borrowSet.getLocalMap().get(place.getLocal());
        if (otherBorrowsOfLocal == null) {
            otherBorrowsOfLocal = Collections.emptySet();
        }

        // If the borrowed place is a local with no projections, all other borrows of this local must conflict.
        // This is purely an optimization, so we don't have to call places_conflict for every borrow.
        if (place.getProjections().isEmpty()) {
            if (!place.getLocal().isRefToStatic()) {
                for (BorrowData borrow : otherBorrowsOfLocal) {
                    state.set(borrow.getIndex(), false);
                }
            }
            return;
        }

        // By passing PlaceConflictBias::NoOverlap, we conservatively assume that any given pair of array indices are
        // not equal, so that when placesConflict returns true, we will be assured that two places being compared
        // definitely denotes the same sets of locations.
        List<BorrowData> definitelyConflictingBorrows = new ArrayList<>();
        for (BorrowData borrow : otherBorrowsOfLocal) {
            // TODO: placesConflict
            if (false) {
                definitelyConflictingBorrows.add(borrow);
            }
        }

        for (BorrowData borrow : definitelyConflictingBorrows) {
            state.set(borrow.getIndex(), false);
        }
    }
}
