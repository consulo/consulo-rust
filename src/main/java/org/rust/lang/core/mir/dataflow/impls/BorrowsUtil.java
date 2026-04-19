/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.mir.dataflow.impls;

import org.jetbrains.annotations.NotNull;
import org.rust.lang.core.mir.dataflow.framework.LocalsStateAtExit;
import org.rust.lang.core.mir.schemas.MirPlace;
import org.rust.lang.core.mir.schemas.MirProjectionElem;
import org.rust.lang.core.mir.schemas.PlaceElem;
import org.rust.lang.core.types.ty.TyPointer;
import org.rust.lang.core.types.ty.TyReference;
import org.rust.lang.core.types.ty.Ty;

import java.util.List;

public final class BorrowsUtil {
    private BorrowsUtil() {
    }

    public static boolean ignoreBorrow(@NotNull MirPlace place, @NotNull LocalsStateAtExit localsStateAtExit) {
        // If a local variable is immutable, then we only need to track borrows to guard against two kinds of errors:
        // * The variable being dropped while still borrowed (e.g., because the fn returns a reference to a local variable)
        // * The variable being moved while still borrowed
        //
        // In particular, the variable cannot be mutated -- the "access checks" will fail -- so we don't have to worry about
        // mutation while borrowed.
        if (localsStateAtExit instanceof LocalsStateAtExit.SomeAreInvalidated) {
            LocalsStateAtExit.SomeAreInvalidated some = (LocalsStateAtExit.SomeAreInvalidated) localsStateAtExit;
            boolean ignore = !some.getHasStorageDeadOrMoved().get(place.getLocal().getIndex())
                && !place.getLocal().getMutability().isMut();
            if (ignore) return true;
        }

        // TODO: support projections when they appear in MIR
        List<? extends PlaceElem> projections = place.getProjections();
        PlaceElem projection = projections.isEmpty() ? null : projections.get(0);
        if (projection instanceof MirProjectionElem.Deref) {
            Ty ty = place.getLocal().getTy();
            if (ty instanceof TyReference && !((TyReference) ty).getMutability().isMut()) {
                // For references to thread-local statics, we do need to track the borrow.
                if (!place.getLocal().isRefToThreadLocal()) return false;
            } else if (ty instanceof TyPointer || (ty instanceof TyReference && !((TyReference) ty).getMutability().isMut())) {
                // For both derefs of raw pointers and &T references, the original path is Copy and therefore
                // not significant. In particular, there is nothing the user can do to the original path that would
                // invalidate the newly created reference -- and if there were, then the user could have copied the
                // original path into a new variable and borrowed *that* one, leaving the original path unborrowed.
                return true;
            }
        }

        return false;
    }
}
