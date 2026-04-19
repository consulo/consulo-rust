/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.typing.paste;

public abstract class DataType {

    /**
     * Return the first non-nullable type contained in this type.
     */
    public DataType unwrap() {
        return this;
    }

    public static final class IntegerType extends DataType {
        public static final IntegerType INSTANCE = new IntegerType();
        private IntegerType() {}
    }

    public static final class FloatType extends DataType {
        public static final FloatType INSTANCE = new FloatType();
        private FloatType() {}
    }

    public static final class BooleanType extends DataType {
        public static final BooleanType INSTANCE = new BooleanType();
        private BooleanType() {}
    }

    public static final class StringType extends DataType {
        public static final StringType INSTANCE = new StringType();
        private StringType() {}
    }

    public static final class Unknown extends DataType {
        public static final Unknown INSTANCE = new Unknown();
        private Unknown() {}
    }

    public static final class StructRef extends DataType {
        private final Struct myStruct;

        public StructRef(Struct struct) {
            myStruct = struct;
        }

        public Struct getStruct() {
            return myStruct;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            StructRef structRef = (StructRef) o;
            return myStruct.equals(structRef.myStruct);
        }

        @Override
        public int hashCode() {
            return myStruct.hashCode();
        }
    }

    public static final class Array extends DataType {
        private final DataType myType;

        public Array(DataType type) {
            myType = type;
        }

        public DataType getType() {
            return myType;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Array array = (Array) o;
            return myType.equals(array.myType);
        }

        @Override
        public int hashCode() {
            return myType.hashCode();
        }
    }

    public static final class Nullable extends DataType {
        private final DataType myType;

        public Nullable(DataType type) {
            myType = type;
        }

        public DataType getType() {
            return myType;
        }

        @Override
        public DataType unwrap() {
            return myType;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Nullable nullable = (Nullable) o;
            return myType.equals(nullable.myType);
        }

        @Override
        public int hashCode() {
            return myType.hashCode();
        }
    }
}
