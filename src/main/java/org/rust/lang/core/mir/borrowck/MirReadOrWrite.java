/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.mir.borrowck;

import org.jetbrains.annotations.NotNull;
import org.rust.lang.core.mir.dataflow.framework.BorrowData;

import java.util.Objects;

/**
 * Kind of access to a value: read or write (For informational purposes only).
 */
public abstract class MirReadOrWrite {
    private MirReadOrWrite() {
    }

    /** From the RFC: "A *read* means that the existing data may be read, but will not be changed." */
    public static final class Read extends MirReadOrWrite {
        @NotNull
        private final MirReadKind kind;

        public Read(@NotNull MirReadKind kind) {
            this.kind = kind;
        }

        @NotNull
        public MirReadKind getKind() {
            return kind;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Read read = (Read) o;
            return Objects.equals(kind, read.kind);
        }

        @Override
        public int hashCode() {
            return Objects.hash(kind);
        }

        @Override
        public String toString() {
            return "Read(kind=" + kind + ")";
        }
    }

    /**
     * From the RFC: "A *write* means that the data may be mutated to new values or otherwise invalidated (for example,
     * it could be de-initialized, as in a move operation).
     */
    public static final class Write extends MirReadOrWrite {
        @NotNull
        private final MirWriteKind kind;

        public Write(@NotNull MirWriteKind kind) {
            this.kind = kind;
        }

        @NotNull
        public MirWriteKind getKind() {
            return kind;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Write write = (Write) o;
            return Objects.equals(kind, write.kind);
        }

        @Override
        public int hashCode() {
            return Objects.hash(kind);
        }

        @Override
        public String toString() {
            return "Write(kind=" + kind + ")";
        }
    }

    /**
     * For two-phase borrows, we distinguish a reservation (which is treated like a Read) from an activation (which is
     * treated like a write), and each of those is furthermore distinguished from Reads/Writes above.
     */
    public static final class Reservation extends MirReadOrWrite {
        @NotNull
        private final MirWriteKind kind;

        public Reservation(@NotNull MirWriteKind kind) {
            this.kind = kind;
        }

        @NotNull
        public MirWriteKind getKind() {
            return kind;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Reservation that = (Reservation) o;
            return Objects.equals(kind, that.kind);
        }

        @Override
        public int hashCode() {
            return Objects.hash(kind);
        }

        @Override
        public String toString() {
            return "Reservation(kind=" + kind + ")";
        }
    }

    public static final class Activation extends MirReadOrWrite {
        @NotNull
        private final MirWriteKind kind;
        @NotNull
        private final BorrowData borrow;

        public Activation(@NotNull MirWriteKind kind, @NotNull BorrowData borrow) {
            this.kind = kind;
            this.borrow = borrow;
        }

        @NotNull
        public MirWriteKind getKind() {
            return kind;
        }

        @NotNull
        public BorrowData getBorrow() {
            return borrow;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Activation that = (Activation) o;
            return Objects.equals(kind, that.kind) && Objects.equals(borrow, that.borrow);
        }

        @Override
        public int hashCode() {
            return Objects.hash(kind, borrow);
        }

        @Override
        public String toString() {
            return "Activation(kind=" + kind + ", borrow=" + borrow + ")";
        }
    }
}
