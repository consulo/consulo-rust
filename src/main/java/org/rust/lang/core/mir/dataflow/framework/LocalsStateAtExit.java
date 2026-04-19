/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.mir.dataflow.framework;

import org.jetbrains.annotations.NotNull;
import org.rust.lang.core.mir.dataflow.move.MoveData;
import org.rust.lang.core.mir.dataflow.move.MoveOut;
import org.rust.lang.core.mir.schemas.*;

import java.util.BitSet;
import java.util.List;
import java.util.Objects;

public abstract class LocalsStateAtExit {
    private LocalsStateAtExit() {
    }

    public static final LocalsStateAtExit ALL_ARE_INVALIDATED = new LocalsStateAtExit() {
        @Override
        public String toString() {
            return "AllAreInvalidated";
        }
    };

    public static final class SomeAreInvalidated extends LocalsStateAtExit {
        @NotNull
        private final BitSet hasStorageDeadOrMoved;

        public SomeAreInvalidated(@NotNull BitSet hasStorageDeadOrMoved) {
            this.hasStorageDeadOrMoved = hasStorageDeadOrMoved;
        }

        @NotNull
        public BitSet getHasStorageDeadOrMoved() {
            return hasStorageDeadOrMoved;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            SomeAreInvalidated that = (SomeAreInvalidated) o;
            return Objects.equals(hasStorageDeadOrMoved, that.hasStorageDeadOrMoved);
        }

        @Override
        public int hashCode() {
            return Objects.hash(hasStorageDeadOrMoved);
        }

        @Override
        public String toString() {
            return "SomeAreInvalidated(hasStorageDeadOrMoved=" + hasStorageDeadOrMoved + ")";
        }
    }

    @NotNull
    public static LocalsStateAtExit build(
        boolean localsAreInvalidatedAtExit,
        @NotNull MirBody body,
        @NotNull MoveData moveData
    ) {
        if (localsAreInvalidatedAtExit) return ALL_ARE_INVALIDATED;

        BitSet hasStorageDeadOrMoved = new BitSet(body.getLocalDecls().size());

        MirVisitor visitor = new MirVisitor() {
            @Override
            @NotNull
            public MirLocal returnPlace() {
                return body.returnPlace();
            }

            @Override
            public void visitLocal(@NotNull MirLocal local, @NotNull MirPlaceContext context, @NotNull MirLocation location) {
                if (context instanceof MirPlaceContext.NonUse.StorageDead) {
                    hasStorageDeadOrMoved.set(local.getIndex(), true);
                }
            }
        };
        visitor.visitBody(body);

        for (List<MoveOut> moveOuts : moveData.getLocMap().values()) {
            for (MoveOut moveOut : moveOuts) { // TODO: attention
                MirLocal local = moveData.baseLocal(moveOut.getPath());
                if (local != null) {
                    hasStorageDeadOrMoved.set(local.getIndex(), true);
                }
            }
        }

        return new SomeAreInvalidated(hasStorageDeadOrMoved);
    }
}
