/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.mir.dataflow.framework;

import jakarta.annotation.Nonnull;
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
    @Nonnull
    private final MirLocation reserveLocation;

    /** Location where the borrow is activated */
    @Nonnull
    private TwoPhaseActivation activationLocation;

    /** What kind of borrow this is */
    @Nonnull
    private final MirBorrowKind kind;

    /** Place from which we are borrowing */
    @Nonnull
    private final MirPlace borrowedPlace;

    /** Place to which the borrow was stored */
    @Nonnull
    private final MirPlace assignedPlace;

    public BorrowData(
        int index,
        @Nonnull MirLocation reserveLocation,
        @Nonnull TwoPhaseActivation activationLocation,
        @Nonnull MirBorrowKind kind,
        @Nonnull MirPlace borrowedPlace,
        @Nonnull MirPlace assignedPlace
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

    @Nonnull
    public MirLocation getReserveLocation() {
        return reserveLocation;
    }

    @Nonnull
    public TwoPhaseActivation getActivationLocation() {
        return activationLocation;
    }

    public void setActivationLocation(@Nonnull TwoPhaseActivation activationLocation) {
        this.activationLocation = activationLocation;
    }

    @Nonnull
    public MirBorrowKind getKind() {
        return kind;
    }

    @Nonnull
    public MirPlace getBorrowedPlace() {
        return borrowedPlace;
    }

    @Nonnull
    public MirPlace getAssignedPlace() {
        return assignedPlace;
    }
}
