/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.mir.schemas;

import org.jetbrains.annotations.NotNull;
import org.rust.lang.core.mir.RsBindingModeWrapper;
import org.rust.lang.core.types.ty.Mutability;

import java.util.Objects;

public abstract class MirBindingMode {
    private MirBindingMode() {
    }

    public static final class BindByReference extends MirBindingMode {
        @NotNull
        private final Mutability mutability;

        public BindByReference(@NotNull Mutability mutability) {
            this.mutability = mutability;
        }

        @NotNull
        public Mutability getMutability() {
            return mutability;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            BindByReference that = (BindByReference) o;
            return Objects.equals(mutability, that.mutability);
        }

        @Override
        public int hashCode() {
            return Objects.hash(mutability);
        }

        @Override
        public String toString() {
            return "BindByReference(mutability=" + mutability + ")";
        }
    }

    public static final class BindByValue extends MirBindingMode {
        @NotNull
        private final Mutability mutability;

        public BindByValue(@NotNull Mutability mutability) {
            this.mutability = mutability;
        }

        @NotNull
        public Mutability getMutability() {
            return mutability;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            BindByValue that = (BindByValue) o;
            return Objects.equals(mutability, that.mutability);
        }

        @Override
        public int hashCode() {
            return Objects.hash(mutability);
        }

        @Override
        public String toString() {
            return "BindByValue(mutability=" + mutability + ")";
        }
    }

    @NotNull
    public static MirBindingMode from(@NotNull RsBindingModeWrapper rsBindingMode) {
        if (rsBindingMode.getRef() == null) {
            return new BindByValue(rsBindingMode.getMutability());
        } else {
            return new BindByReference(rsBindingMode.getMutability());
        }
    }
}
