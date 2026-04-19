/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.mir.dataflow.framework;

import org.jetbrains.annotations.NotNull;
import org.rust.lang.core.mir.dataflow.impls.BorrowsUtil;
import org.rust.lang.core.mir.schemas.*;
import org.rust.lang.core.mir.util.IndexAlloc;
import org.rust.lang.core.mir.util.IndexKeyMap;

import java.util.*;

public class GatherBorrows implements MirVisitor {
    @NotNull
    private final MirBody body;
    @NotNull
    private final Map<MirLocation, BorrowData> locationMap;
    @NotNull
    private final Map<MirLocation, List<BorrowData>> activationMap;
    @NotNull
    private final IndexKeyMap<MirLocal, Set<BorrowData>> localMap;

    /**
     * When we encounter a 2-phase borrow statement, it will always be assigning into a temporary TEMP:
     *
     * TEMP = &foo
     *
     * We add TEMP into this map with b, where b is the index of the borrow. When we find a later use of this
     * activation, we remove from the map (and add to the "tombstone" set below).
     */
    @NotNull
    private final IndexKeyMap<MirLocal, BorrowData> pendingActivations;

    @NotNull
    private final LocalsStateAtExit localsStateAtExit;
    @NotNull
    private final IndexAlloc<BorrowData> borrowData;

    public GatherBorrows(
        @NotNull MirBody body,
        @NotNull Map<MirLocation, BorrowData> locationMap,
        @NotNull Map<MirLocation, List<BorrowData>> activationMap,
        @NotNull IndexKeyMap<MirLocal, Set<BorrowData>> localMap,
        @NotNull IndexKeyMap<MirLocal, BorrowData> pendingActivations,
        @NotNull LocalsStateAtExit localsStateAtExit,
        @NotNull IndexAlloc<BorrowData> borrowData
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
    @NotNull
    public MirLocal returnPlace() {
        return body.returnPlace();
    }

    @Override
    public void visitAssign(@NotNull MirPlace place, @NotNull MirRvalue rvalue, @NotNull MirLocation location) {
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
    public void visitLocal(@NotNull MirLocal local, @NotNull MirPlaceContext context, @NotNull MirLocation location) {
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

    private void insertAsPendingIfTwoPhase(@NotNull MirPlace assignedPlace, @NotNull MirBorrowKind kind, @NotNull BorrowData bd) {
        if (!kind.getAllowTwoPhaseBorrow()) return;

        // Consider the borrow not activated to start. When we find an activation, we'll update this field.
        bd.setActivationLocation(TwoPhaseActivation.NOT_ACTIVATED);

        // Insert local into the list of pending activations. From now on, we'll be on the lookout for a use of it.
        // Note that we are guaranteed that this use will come after the assignment.
        pendingActivations.put(assignedPlace.getLocal(), bd);
    }

    @NotNull
    public Map<MirLocation, BorrowData> getLocationMap() {
        return locationMap;
    }

    @NotNull
    public Map<MirLocation, List<BorrowData>> getActivationMap() {
        return activationMap;
    }

    @NotNull
    public IndexKeyMap<MirLocal, Set<BorrowData>> getLocalMap() {
        return localMap;
    }

    @NotNull
    public LocalsStateAtExit getLocalsStateAtExit() {
        return localsStateAtExit;
    }

    @NotNull
    public IndexAlloc<BorrowData> getBorrowData() {
        return borrowData;
    }
}
