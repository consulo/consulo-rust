/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.mir.schemas;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

// https://github.com/rust-lang/rust/blob/f7b831ac8a897273f78b9f47165cf8e54066ce4b/compiler/rustc_middle/src/mir/interpret/value.rs#L114
public abstract class MirScalar {
    private MirScalar() {
    }

    public static final class Int extends MirScalar {
        @NotNull
        private final MirScalarInt scalarInt;

        public Int(@NotNull MirScalarInt scalarInt) {
            this.scalarInt = scalarInt;
        }

        @NotNull
        public MirScalarInt getScalarInt() {
            return scalarInt;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Int anInt = (Int) o;
            return Objects.equals(scalarInt, anInt.scalarInt);
        }

        @Override
        public int hashCode() {
            return Objects.hash(scalarInt);
        }

        @Override
        public String toString() {
            return "Int(scalarInt=" + scalarInt + ")";
        }
    }

    @Nullable
    public MirScalarInt tryToInt() {
        if (this instanceof Int) {
            return ((Int) this).getScalarInt();
        }
        return null;
    }

    // TODO: there is error handling done here
    public long toBits() {
        MirScalarInt scalarInt = tryToInt();
        if (scalarInt == null) {
            throw new IllegalStateException("Could not get bits from scalar");
        }
        return scalarInt.toBits();
    }

    public boolean toBool() {
        long bits = toBits();
        if (bits == 0L) {
            return false;
        } else if (bits == 1L) {
            return true;
        } else {
            throw new IllegalStateException("Cannot translate to bool");
        }
    }

    @NotNull
    public static MirScalar from(boolean bool) {
        return new Int(new MirScalarInt(bool ? 1 : 0, (byte) 0)); // TODO: size is not used anywhere
    }

    @NotNull
    public static MirScalar from(long value) {
        return from(value, (byte) 0);
    }

    @NotNull
    public static MirScalar from(long value, byte size) {
        return new Int(new MirScalarInt(value, size));
    }
}
