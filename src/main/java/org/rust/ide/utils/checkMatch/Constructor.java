/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.utils.checkMatch;

import org.jetbrains.annotations.NotNull;
import org.rust.lang.core.psi.RsEnumItem;
import org.rust.lang.core.psi.RsEnumVariant;
import org.rust.lang.core.psi.RsStructItem;
import org.rust.lang.core.psi.ext.RsFieldsOwner;
import org.rust.lang.core.psi.ext.RsFieldsOwnerUtil;
import org.rust.lang.core.psi.ext.RsStubbedElementKindUtil;
import org.rust.lang.core.types.infer.TypeInference;
import org.rust.lang.core.types.ty.*;
import org.rust.lang.utils.evaluation.ConstExpr;

import java.util.*;
import java.util.stream.Collectors;
import org.rust.lang.core.psi.ext.RsEnumItemUtil;
import org.rust.lang.core.types.RsTypesUtil;
import org.rust.lang.core.types.infer.FoldUtil;

public abstract class Constructor {
    private Constructor() {
    }

    /** The constructor of all patterns that don't vary by constructor, e.g. struct patterns and fixed-length arrays */
    public static final class Single extends Constructor {
        public static final Single INSTANCE = new Single();

        private Single() {
        }

        @Override
        public boolean coveredByRange(@NotNull ConstExpr.Value<?> from, @NotNull ConstExpr.Value<?> to, boolean included) {
            return true;
        }

        @Override
        public boolean equals(Object obj) {
            return obj instanceof Single;
        }

        @Override
        public int hashCode() {
            return 0;
        }

        @Override
        public String toString() {
            return "Single";
        }
    }

    /** Enum variants */
    public static final class Variant extends Constructor {
        @NotNull
        private final RsEnumVariant variant;

        public Variant(@NotNull RsEnumVariant variant) {
            this.variant = variant;
        }

        @NotNull
        public RsEnumVariant getVariant() {
            return variant;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof Variant)) return false;
            return variant.equals(((Variant) o).variant);
        }

        @Override
        public int hashCode() {
            return variant.hashCode();
        }

        @Override
        public String toString() {
            return "Variant(variant=" + variant + ")";
        }
    }

    /** Literal values */
    public static final class ConstantValue extends Constructor {
        @NotNull
        private final ConstExpr.Value<?> value;

        public ConstantValue(@NotNull ConstExpr.Value<?> value) {
            this.value = value;
        }

        @NotNull
        public ConstExpr.Value<?> getValue() {
            return value;
        }

        @Override
        public boolean coveredByRange(@NotNull ConstExpr.Value<?> from, @NotNull ConstExpr.Value<?> to, boolean included) {
            if (included) {
                return compareValues(value, from) >= 0 && compareValues(value, to) <= 0;
            } else {
                return compareValues(value, from) >= 0 && compareValues(value, to) < 0;
            }
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof ConstantValue)) return false;
            return value.equals(((ConstantValue) o).value);
        }

        @Override
        public int hashCode() {
            return value.hashCode();
        }

        @Override
        public String toString() {
            return "ConstantValue(value=" + value + ")";
        }
    }

    /** Ranges of literal values ({@code 2..=5} and {@code 2..5}) */
    public static final class ConstantRange extends Constructor {
        @NotNull
        private final ConstExpr.Value<?> start;
        @NotNull
        private final ConstExpr.Value<?> end;
        private final boolean includeEnd;

        public ConstantRange(@NotNull ConstExpr.Value<?> start, @NotNull ConstExpr.Value<?> end, boolean includeEnd) {
            this.start = start;
            this.end = end;
            this.includeEnd = includeEnd;
        }

        public ConstantRange(@NotNull ConstExpr.Value<?> start, @NotNull ConstExpr.Value<?> end) {
            this(start, end, false);
        }

        @NotNull
        public ConstExpr.Value<?> getStart() {
            return start;
        }

        @NotNull
        public ConstExpr.Value<?> getEnd() {
            return end;
        }

        public boolean isIncludeEnd() {
            return includeEnd;
        }

        @Override
        public boolean coveredByRange(@NotNull ConstExpr.Value<?> from, @NotNull ConstExpr.Value<?> to, boolean included) {
            if (includeEnd) {
                return ((compareValues(end, to) < 0) || (included && compareValues(to, end) == 0)) && (compareValues(start, from) >= 0);
            } else {
                return ((compareValues(end, to) < 0) || (!included && compareValues(to, end) == 0)) && (compareValues(start, from) >= 0);
            }
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof ConstantRange)) return false;
            ConstantRange that = (ConstantRange) o;
            return includeEnd == that.includeEnd && start.equals(that.start) && end.equals(that.end);
        }

        @Override
        public int hashCode() {
            return Objects.hash(start, end, includeEnd);
        }

        @Override
        public String toString() {
            return "ConstantRange(start=" + start + ", end=" + end + ", includeEnd=" + includeEnd + ")";
        }
    }

    /** Array patterns of length n */
    public static final class Slice extends Constructor {
        private final int size;

        public Slice(int size) {
            this.size = size;
        }

        public int getSize() {
            return size;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof Slice)) return false;
            return size == ((Slice) o).size;
        }

        @Override
        public int hashCode() {
            return size;
        }

        @Override
        public String toString() {
            return "Slice(size=" + size + ")";
        }
    }

    public int arity(@NotNull Ty type) {
        if (type instanceof TyTuple) {
            return ((TyTuple) type).getTypes().size();
        }
        if (type instanceof TySlice || type instanceof TyArray) {
            if (this instanceof Slice) {
                return ((Slice) this).getSize();
            }
            if (this instanceof ConstantValue) {
                return 0;
            }
            throw new CheckMatchRuntimeException("Incompatible constructor");
        }
        if (type instanceof TyReference) {
            return 1;
        }
        if (type instanceof TyAdt) {
            TyAdt adt = (TyAdt) type;
            if (adt.getItem() instanceof RsStructItem) {
                return RsFieldsOwnerUtil.getSize((RsFieldsOwner) adt.getItem());
            }
            if (adt.getItem() instanceof RsEnumItem && this instanceof Variant) {
                return RsFieldsOwnerUtil.getSize(((Variant) this).getVariant());
            }
            throw new CheckMatchRuntimeException("Incompatible constructor");
        }
        return 0;
    }

    public boolean coveredByRange(@NotNull ConstExpr.Value<?> from, @NotNull ConstExpr.Value<?> to, boolean included) {
        return false;
    }

    @NotNull
    public List<Ty> subTypes(@NotNull Ty type) {
        if (type instanceof TyTuple) {
            return ((TyTuple) type).getTypes();
        }
        if (type instanceof TySlice || type instanceof TyArray) {
            if (this instanceof Slice) {
                int sliceSize = ((Slice) this).getSize();
                List<Ty> result = new ArrayList<>(sliceSize);
                for (int i = 0; i < sliceSize; i++) {
                    result.add(type);
                }
                return result;
            }
            if (this instanceof ConstantValue) {
                return Collections.emptyList();
            }
            throw new CheckMatchRuntimeException("Incompatible constructor");
        }
        if (type instanceof TyReference) {
            return Collections.singletonList(((TyReference) type).getReferenced());
        }
        if (type instanceof TyAdt) {
            TyAdt adt = (TyAdt) type;
            if (this instanceof Single && adt.getItem() instanceof RsFieldsOwner) {
                return RsFieldsOwnerUtil.getFieldTypes((RsFieldsOwner) adt.getItem()).stream()
                    .map(fieldTy -> FoldUtil.substitute(fieldTy, adt.getTypeParameterValues()))
                    .collect(Collectors.toList());
            }
            if (this instanceof Variant) {
                return RsFieldsOwnerUtil.getFieldTypes(((Variant) this).getVariant()).stream()
                    .map(fieldTy -> FoldUtil.substitute(fieldTy, adt.getTypeParameterValues()))
                    .collect(Collectors.toList());
            }
            return Collections.emptyList();
        }
        return Collections.emptyList();
    }

    public static boolean isInhabited(@NotNull Ty ty) {
        return allConstructorsLazy(ty).iterator().hasNext();
    }

    @NotNull
    public static List<Constructor> allConstructors(@NotNull Ty ty) {
        List<Constructor> result = new ArrayList<>();
        allConstructorsLazy(ty).forEach(result::add);
        return result;
    }

    @NotNull
    private static Iterable<Constructor> allConstructorsLazy(@NotNull Ty ty) {
        if (ty instanceof TyBool) {
            return Arrays.asList(
                new ConstantValue(new ConstExpr.Value.Bool(true)),
                new ConstantValue(new ConstExpr.Value.Bool(false))
            );
        }
        if (ty instanceof TyAdt && ((TyAdt) ty).getItem() instanceof RsEnumItem) {
            RsEnumItem enumItem = (RsEnumItem) ((TyAdt) ty).getItem();
            return RsEnumItemUtil.getVariants(enumItem).stream()
                .map(Variant::new)
                .collect(Collectors.toList());
        }
        if (ty instanceof TyArray && ((TyArray) ty).getSize() != null) {
            throw new UnsupportedOperationException("TODO: Array constructors");
        }
        if (ty instanceof TyArray || ty instanceof TySlice) {
            throw new UnsupportedOperationException("TODO: Slice/Array constructors");
        }
        return Collections.singletonList(Single.INSTANCE);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    static int compareValues(@NotNull ConstExpr.Value<?> a, @NotNull ConstExpr.Value<?> b) {
        if (a instanceof ConstExpr.Value.Bool && b instanceof ConstExpr.Value.Bool) {
            return Boolean.compare(((ConstExpr.Value.Bool) a).getValue(), ((ConstExpr.Value.Bool) b).getValue());
        }
        if (a instanceof ConstExpr.Value.Integer && b instanceof ConstExpr.Value.Integer) {
            return Long.compare(((ConstExpr.Value.Integer) a).getValue(), ((ConstExpr.Value.Integer) b).getValue());
        }
        if (a instanceof ConstExpr.Value.Float && b instanceof ConstExpr.Value.Float) {
            return Double.compare(((ConstExpr.Value.Float) a).getValue(), ((ConstExpr.Value.Float) b).getValue());
        }
        if (a instanceof ConstExpr.Value.Str && b instanceof ConstExpr.Value.Str) {
            return ((Comparable) ((ConstExpr.Value.Str) a).getValue()).compareTo(((ConstExpr.Value.Str) b).getValue());
        }
        if (a instanceof ConstExpr.Value.Char && b instanceof ConstExpr.Value.Char) {
            return ((Comparable) ((ConstExpr.Value.Char) a).getValue()).compareTo(((ConstExpr.Value.Char) b).getValue());
        }
        throw new CheckMatchRuntimeException("Comparison of incompatible types: " + a.getClass() + " and " + b.getClass());
    }

    /**
     * but throws it from contexts that don't declare it).
     */
    static class CheckMatchRuntimeException extends RuntimeException {
        CheckMatchRuntimeException(String message) {
            super(message);
        }
    }
}
