/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.mir.borrowck;

import org.jetbrains.annotations.NotNull;
import org.rust.lang.core.mir.schemas.MirBorrowKind;

import java.util.Objects;

/**
 * Kind of read access to a value (For informational purposes only).
 */
public abstract class MirReadKind {
    private MirReadKind() {
    }

    public static final class Borrow extends MirReadKind {
        @NotNull
        private final MirBorrowKind kind;

        public Borrow(@NotNull MirBorrowKind kind) {
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
            Borrow borrow = (Borrow) o;
            return Objects.equals(kind, borrow.kind);
        }

        @Override
        public int hashCode() {
            return Objects.hash(kind);
        }

        @Override
        public String toString() {
            return "Borrow(kind=" + kind + ")";
        }
    }

    public static final class Copy extends MirReadKind {
        public static final Copy INSTANCE = new Copy();

        private Copy() {
        }

        @Override
        public String toString() {
            return "Copy";
        }
    }
}
