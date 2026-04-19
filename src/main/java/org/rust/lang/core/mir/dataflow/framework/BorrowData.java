/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.mir.dataflow.framework;

import org.jetbrains.annotations.NotNull;
import org.rust.lang.core.mir.WithIndex;
import org.rust.lang.core.mir.schemas.MirBorrowKind;
import org.rust.lang.core.mir.schemas.MirLocation;
import org.rust.lang.core.mir.schemas.MirPlace;

public class BorrowData implements WithIndex {
    private final int index;

    /**
     * Location where the borrow reservation starts.
     * In many cases, this will be equal to the activation location but not always.
     */
    @NotNull
    private final MirLocation reserveLocation;

    /** Location where the borrow is activated */
    @NotNull
    private TwoPhaseActivation activationLocation;

    /** What kind of borrow this is */
    @NotNull
    private final MirBorrowKind kind;

    /** Place from which we are borrowing */
    @NotNull
    private final MirPlace borrowedPlace;

    /** Place to which the borrow was stored */
    @NotNull
    private final MirPlace assignedPlace;

    public BorrowData(
        int index,
        @NotNull MirLocation reserveLocation,
        @NotNull TwoPhaseActivation activationLocation,
        @NotNull MirBorrowKind kind,
        @NotNull MirPlace borrowedPlace,
        @NotNull MirPlace assignedPlace
    ) {
        this.index = index;
        this.reserveLocation = reserveLocation;
        this.activationLocation = activationLocation;
        this.kind = kind;
        this.borrowedPlace = borrowedPlace;
        this.assignedPlace = assignedPlace;
    }

    @Override
    public int getIndex() {
        return index;
    }

    @NotNull
    public MirLocation getReserveLocation() {
        return reserveLocation;
    }

    @NotNull
    public TwoPhaseActivation getActivationLocation() {
        return activationLocation;
    }

    public void setActivationLocation(@NotNull TwoPhaseActivation activationLocation) {
        this.activationLocation = activationLocation;
    }

    @NotNull
    public MirBorrowKind getKind() {
        return kind;
    }

    @NotNull
    public MirPlace getBorrowedPlace() {
        return borrowedPlace;
    }

    @NotNull
    public MirPlace getAssignedPlace() {
        return assignedPlace;
    }
}
