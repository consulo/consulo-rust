/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.mir.schemas;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.util.Objects;

public abstract class MirProjectionElem<T> implements PlaceElem {
    private MirProjectionElem() {
    }

    @SuppressWarnings("rawtypes")
    public static final class Deref extends MirProjectionElem {
        public static final Deref INSTANCE = new Deref();

        private Deref() {
        }

        @Override
        public String toString() {
            return "Deref";
        }
    }

    public static final class Field<T> extends MirProjectionElem<T> {
        private final int fieldIndex;
        @NotNull
        private final T elem;

        public Field(int fieldIndex, @NotNull T elem) {
            this.fieldIndex = fieldIndex;
            this.elem = elem;
        }

        public int getFieldIndex() {
            return fieldIndex;
        }

        @NotNull
        public T getElem() {
            return elem;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Field<?> field = (Field<?>) o;
            return fieldIndex == field.fieldIndex && Objects.equals(elem, field.elem);
        }

        @Override
        public int hashCode() {
            return Objects.hash(fieldIndex, elem);
        }

        @Override
        public String toString() {
            return "Field(fieldIndex=" + fieldIndex + ", elem=" + elem + ")";
        }
    }

    public static final class Index<T> extends MirProjectionElem<T> {
        @NotNull
        private final MirLocal index;

        public Index(@NotNull MirLocal index) {
            this.index = index;
        }

        @NotNull
        public MirLocal getIndex() {
            return index;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Index<?> index1 = (Index<?>) o;
            return Objects.equals(index, index1.index);
        }

        @Override
        public int hashCode() {
            return Objects.hash(index);
        }

        @Override
        public String toString() {
            return "Index(index=" + index + ")";
        }
    }

    public static final class ConstantIndex<T> extends MirProjectionElem<T> {
        private final long offset;
        private final long minLength;
        private final boolean fromEnd;

        public ConstantIndex(long offset, long minLength, boolean fromEnd) {
            this.offset = offset;
            this.minLength = minLength;
            this.fromEnd = fromEnd;
        }

        public long getOffset() {
            return offset;
        }

        public long getMinLength() {
            return minLength;
        }

        public boolean isFromEnd() {
            return fromEnd;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            ConstantIndex<?> that = (ConstantIndex<?>) o;
            return offset == that.offset && minLength == that.minLength && fromEnd == that.fromEnd;
        }

        @Override
        public int hashCode() {
            return Objects.hash(offset, minLength, fromEnd);
        }

        @Override
        public String toString() {
            return "ConstantIndex(offset=" + offset + ", minLength=" + minLength + ", fromEnd=" + fromEnd + ")";
        }
    }

    /**
     * "Downcast" to a variant of an enum or a generator.
     * name is the name of the variant, used for printing MIR.
     */
    public static final class Downcast<T> extends MirProjectionElem<T> {
        @Nullable
        private final String name;
        private final int variantIndex;

        public Downcast(@Nullable String name, int variantIndex) {
            this.name = name;
            this.variantIndex = variantIndex;
        }

        @Nullable
        public String getName() {
            return name;
        }

        public int getVariantIndex() {
            return variantIndex;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Downcast<?> downcast = (Downcast<?>) o;
            return variantIndex == downcast.variantIndex && Objects.equals(name, downcast.name);
        }

        @Override
        public int hashCode() {
            return Objects.hash(name, variantIndex);
        }

        @Override
        public String toString() {
            return "Downcast(name=" + name + ", variantIndex=" + variantIndex + ")";
        }
    }
}
