/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.mir.dataflow.framework;

import jakarta.annotation.Nonnull;
import org.rust.lang.core.mir.dataflow.impls.BorrowsUtil;
import org.rust.lang.core.mir.schemas.*;
import org.rust.lang.core.mir.util.IndexAlloc;
import org.rust.lang.core.mir.util.IndexKeyMap;

import java.util.*;

public class GatherBorrows implements MirVisitor {
    @Nonnull
    private final MirBody body;
    @Nonnull
    private final Map<MirLocation, BorrowData> locationMap;
    @Nonnull
    private final Map<MirLocation, List<BorrowData>> activationMap;
    @Nonnull
    private final IndexKeyMap<MirLocal, Set<BorrowData>> localMap;

    /**
     * When we encounter a 2-phase borrow statement, it will always be assigning into a temporary TEMP:
     *
     * TEMP = &foo
     *
     * We add TEMP into this map with b, where b is the index of the borrow. When we find a later use of this
     * activation, we remove from the map (and add to the "tombstone" set below).
     */
    @Nonnull
    private final IndexKeyMap<MirLocal, BorrowData> pendingActivations;

    @Nonnull
    private final LocalsStateAtExit localsStateAtExit;
    @Nonnull
    private final IndexAlloc<BorrowData> borrowData;

    public GatherBorrows(
        @Nonnull MirBody body,
        @Nonnull Map<MirLocation, BorrowData> locationMap,
        @Nonnull Map<MirLocation, List<BorrowData>> activationMap,
        @Nonnull IndexKeyMap<MirLocal, Set<BorrowData>> localMap,
        @Nonnull IndexKeyMap<MirLocal, BorrowData> pendingActivations,
        @Nonnull LocalsStateAtExit localsStateAtExit,
        @Nonnull IndexAlloc<BorrowData> borrowData
    ) {
        this.body = body;
        this.locationMap = locationMap;
        this.activationMap = activationMap;
        this.localMap = localMap;
        this.pendingActivations = pendingActivations;
        this.localsStateAtExit = localsStateAtExit;
        this.borrowData = borrowData;
    }

    @Override
    @Nonnull
    public MirLocal returnPlace() {
        return body.returnPlace();
    }

    @Override
    public void visitAssign(@Nonnull MirPlace place, @Nonnull MirRvalue rvalue, @Nonnull MirLocation location) {
        if (rvalue instanceof MirRvalue.Ref) {
            MirRvalue.Ref ref = (MirRvalue.Ref) rvalue;
            if (BorrowsUtil.ignoreBorrow(ref.getPlace(), localsStateAtExit)) return;
            BorrowData borrow = borrowData.allocate(index -> new BorrowData(
                index,
                location,
                TwoPhaseActivation.NOT_TWO_PHASE,
                ref.getBorrowKind(),
                ref.getPlace(),
                place
            ));
            BorrowData existing = locationMap.putIfAbsent(location, borrow);
            BorrowData borrowFromMap = existing != null ? existing : borrow;
            insertAsPendingIfTwoPhase(place, ref.getBorrowKind(), borrowFromMap);
            Set<BorrowData> localBorrows = localMap.get(ref.getPlace().getLocal());
            if (localBorrows == null) {
                localBorrows = new HashSet<>();
                localMap.put(ref.getPlace().getLocal(), localBorrows);
            }
            localBorrows.add(borrowFromMap);
        }

        MirVisitor.super.visitAssign(place, rvalue, location);
    }

    @Override
    public void visitLocal(@Nonnull MirLocal local, @Nonnull MirPlaceContext context, @Nonnull MirLocation location) {
        if (!context.isUse()) return;

        // We found a use of some temporary TMP check whether we (earlier) saw a 2-phase borrow like
        //
        //     TMP = &mut place
        BorrowData bd = pendingActivations.get(local);
        if (bd != null) {
            // Watch out: the use of TMP in the borrow itself doesn't count as an activation. =)
            if (bd.getReserveLocation().equals(location) && context instanceof MirPlaceContext.MutatingUse.Store) return;

            activationMap.computeIfAbsent(location, k -> new ArrayList<>()).add(bd);
            bd.setActivationLocation(new TwoPhaseActivation.ActivatedAt(location));
        }
    }

    private void insertAsPendingIfTwoPhase(@Nonnull MirPlace assignedPlace, @Nonnull MirBorrowKind kind, @Nonnull BorrowData bd) {
        if (!kind.getAllowTwoPhaseBorrow()) return;

        // Consider the borrow not activated to start. When we find an activation, we'll update this field.
        bd.setActivationLocation(TwoPhaseActivation.NOT_ACTIVATED);

        // Insert local into the list of pending activations. From now on, we'll be on the lookout for a use of it.
        // Note that we are guaranteed that this use will come after the assignment.
        pendingActivations.put(assignedPlace.getLocal(), bd);
    }

    @Nonnull
    public Map<MirLocation, BorrowData> getLocationMap() {
        return locationMap;
    }

    @Nonnull
    public Map<MirLocation, List<BorrowData>> getActivationMap() {
        return activationMap;
    }

    @Nonnull
    public IndexKeyMap<MirLocal, Set<BorrowData>> getLocalMap() {
        return localMap;
    }

    @Nonnull
    public LocalsStateAtExit getLocalsStateAtExit() {
        return localsStateAtExit;
    }

    @Nonnull
    public IndexAlloc<BorrowData> getBorrowData() {
        return borrowData;
    }
}
