/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.thir;

import jakarta.annotation.Nonnull;
import org.rust.lang.core.mir.schemas.MirBorrowKind;

import java.util.Objects;

public abstract class ThirBindingMode {
    ThirBindingMode() {
    }

    public static final class ByValue extends ThirBindingMode {
        public static final ByValue INSTANCE = new ByValue();
        private ByValue() {}
    }

    public static class ByRef extends ThirBindingMode {
        @Nonnull
        public final MirBorrowKind kind;

        public ByRef(@Nonnull MirBorrowKind kind) {
            this.kind = kind;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof ByRef)) return false;
            return kind.equals(((ByRef) o).kind);
        }

        @Override
        public int hashCode() {
            return kind.hashCode();
        }
    }
}
