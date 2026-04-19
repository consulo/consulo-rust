/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.mir.schemas;

import org.jetbrains.annotations.NotNull;

import java.util.Objects;

public abstract class MirClearCrossCrate<T> {
    private MirClearCrossCrate() {
    }

    @SuppressWarnings("rawtypes")
    public static final class Clear extends MirClearCrossCrate {
        public static final Clear INSTANCE = new Clear();

        private Clear() {
        }

        @Override
        public String toString() {
            return "Clear";
        }
    }

    public static final class Set<T> extends MirClearCrossCrate<T> {
        @NotNull
        private final T value;

        public Set(@NotNull T value) {
            this.value = value;
        }

        @NotNull
        public T getValue() {
            return value;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Set<?> set = (Set<?>) o;
            return Objects.equals(value, set.value);
        }

        @Override
        public int hashCode() {
            return Objects.hash(value);
        }

        @Override
        public String toString() {
            return "Set(value=" + value + ")";
        }
    }
}
