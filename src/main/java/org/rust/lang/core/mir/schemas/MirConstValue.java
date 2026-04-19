/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.mir.schemas;

import org.jetbrains.annotations.NotNull;

import java.util.Objects;

// https://github.com/rust-lang/rust/blob/f7b831ac8a897273f78b9f47165cf8e54066ce4b/compiler/rustc_middle/src/mir/interpret/value.rs#L32
public abstract class MirConstValue {
    private MirConstValue() {
    }

    public static final class Scalar extends MirConstValue {
        @NotNull
        private final MirScalar value;

        public Scalar(@NotNull MirScalar value) {
            this.value = value;
        }

        @NotNull
        public MirScalar getValue() {
            return value;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Scalar scalar = (Scalar) o;
            return Objects.equals(value, scalar.value);
        }

        @Override
        public int hashCode() {
            return Objects.hash(value);
        }

        @Override
        public String toString() {
            return "Scalar(value=" + value + ")";
        }
    }

    public static final class ZeroSized extends MirConstValue {
        public static final ZeroSized INSTANCE = new ZeroSized();

        private ZeroSized() {
        }

        @Override
        public String toString() {
            return "ZeroSized";
        }
    }
}
