/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.mir.schemas;

import org.jetbrains.annotations.NotNull;
import org.rust.lang.core.types.ty.AutoBorrowMutability;

import java.util.Objects;

public abstract class MirBorrowKind {
    private MirBorrowKind() {
    }

    public boolean getAllowTwoPhaseBorrow() {
        return false;
    }

    public static final class Shared extends MirBorrowKind {
        public static final Shared INSTANCE = new Shared();

        private Shared() {
        }

        @Override
        public String toString() {
            return "Shared";
        }
    }

    public static final class Shallow extends MirBorrowKind {
        public static final Shallow INSTANCE = new Shallow();

        private Shallow() {
        }

        @Override
        public String toString() {
            return "Shallow";
        }
    }

    public static final class Unique extends MirBorrowKind {
        public static final Unique INSTANCE = new Unique();

        private Unique() {
        }

        @Override
        public String toString() {
            return "Unique";
        }
    }

    public static final class Mut extends MirBorrowKind {
        private final boolean allowTwoPhaseBorrow;

        public Mut(boolean allowTwoPhaseBorrow) {
            this.allowTwoPhaseBorrow = allowTwoPhaseBorrow;
        }

        @Override
        public boolean getAllowTwoPhaseBorrow() {
            return allowTwoPhaseBorrow;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Mut mut = (Mut) o;
            return allowTwoPhaseBorrow == mut.allowTwoPhaseBorrow;
        }

        @Override
        public int hashCode() {
            return Objects.hash(allowTwoPhaseBorrow);
        }

        @Override
        public String toString() {
            return "Mut(allowTwoPhaseBorrow=" + allowTwoPhaseBorrow + ")";
        }
    }

    @NotNull
    public static MirBorrowKind toBorrowKind(@NotNull AutoBorrowMutability autoBorrowMutability) {
        if (autoBorrowMutability == AutoBorrowMutability.Immutable) {
            return Shared.INSTANCE;
        } else if (autoBorrowMutability instanceof AutoBorrowMutability.Mutable) {
            return new Mut(((AutoBorrowMutability.Mutable) autoBorrowMutability).isAllowTwoPhaseBorrow());
        } else {
            throw new IllegalStateException("Unknown AutoBorrowMutability: " + autoBorrowMutability);
        }
    }
}
