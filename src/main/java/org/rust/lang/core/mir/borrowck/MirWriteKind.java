/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.mir.borrowck;

import org.jetbrains.annotations.NotNull;
import org.rust.lang.core.mir.schemas.MirBorrowKind;

import java.util.Objects;

/**
 * Kind of write access to a value (For informational purposes only).
 */
public abstract class MirWriteKind {
    private MirWriteKind() {
    }

    public static final class StorageDeadOrDrop extends MirWriteKind {
        public static final StorageDeadOrDrop INSTANCE = new StorageDeadOrDrop();

        private StorageDeadOrDrop() {
        }

        @Override
        public String toString() {
            return "StorageDeadOrDrop";
        }
    }

    public static final class MutableBorrow extends MirWriteKind {
        @NotNull
        private final MirBorrowKind kind;

        public MutableBorrow(@NotNull MirBorrowKind kind) {
            this.kind = kind;
        }

        @NotNull
        public MirBorrowKind getKind() {
            return kind;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            MutableBorrow that = (MutableBorrow) o;
            return Objects.equals(kind, that.kind);
        }

        @Override
        public int hashCode() {
            return Objects.hash(kind);
        }

        @Override
        public String toString() {
            return "MutableBorrow(kind=" + kind + ")";
        }
    }

    public static final class Mutate extends MirWriteKind {
        public static final Mutate INSTANCE = new Mutate();

        private Mutate() {
        }

        @Override
        public String toString() {
            return "Mutate";
        }
    }

    public static final class Move extends MirWriteKind {
        public static final Move INSTANCE = new Move();

        private Move() {
        }

        @Override
        public String toString() {
            return "Move";
        }
    }
}
