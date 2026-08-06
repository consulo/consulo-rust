/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.utils.checkMatch;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.rust.lang.core.psi.RsEnumItem;
import org.rust.lang.core.psi.RsEnumVariant;
import org.rust.lang.core.types.ty.Ty;
import org.rust.lang.utils.evaluation.ConstExpr;

import java.util.List;
import java.util.Objects;

public abstract class PatternKind {
    private PatternKind() {
    }

    public static final PatternKind Wild = new PatternKind() {
        @Override
        public String toString() {
            return "Wild";
        }

        @Override
        public boolean equals(Object obj) {
            return obj == this;
        }

        @Override
        public int hashCode() {
            return System.identityHashCode(this);
        }
    };

    /** x, ref x, x @ P, etc */
    public static final class Binding extends PatternKind {
        @Nonnull
        private final Ty ty;
        @Nonnull
        private final String name;

        public Binding(@Nonnull Ty ty, @Nonnull String name) {
            this.ty = ty;
            this.name = name;
        }

        @Nonnull
        public Ty getTy() {
            return ty;
        }

        @Nonnull
        public String getName() {
            return name;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof Binding)) return false;
            Binding binding = (Binding) o;
            return ty.equals(binding.ty) && name.equals(binding.name);
        }

        @Override
        public int hashCode() {
            return Objects.hash(ty, name);
        }

        @Override
        public String toString() {
            return "Binding(ty=" + ty + ", name=" + name + ")";
        }
    }

    /** Foo(...) or Foo{...} or Foo, where `Foo` is a variant name from an adt with >1 variants (only enums) */
    public static final class Variant extends PatternKind {
        @Nonnull
        private final RsEnumItem item;
        @Nonnull
        private final RsEnumVariant variant;
        @Nonnull
        private final List<Pattern> subPatterns;

        public Variant(@Nonnull RsEnumItem item, @Nonnull RsEnumVariant variant, @Nonnull List<Pattern> subPatterns) {
            this.item = item;
            this.variant = variant;
            this.subPatterns = subPatterns;
        }

        @Nonnull
        public RsEnumItem getItem() {
            return item;
        }

        @Nonnull
        public RsEnumVariant getVariant() {
            return variant;
        }

        @Nonnull
        public List<Pattern> getSubPatterns() {
            return subPatterns;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof Variant)) return false;
            Variant v = (Variant) o;
            return item.equals(v.item) && variant.equals(v.variant) && subPatterns.equals(v.subPatterns);
        }

        @Override
        public int hashCode() {
            return Objects.hash(item, variant, subPatterns);
        }

        @Override
        public String toString() {
            return "Variant(item=" + item + ", variant=" + variant + ", subPatterns=" + subPatterns + ")";
        }
    }

    /** (...), Foo(...), Foo{...}, or Foo, where `Foo` is a variant name from an adt with 1 variant (structs or enums) */
    public static final class Leaf extends PatternKind {
        @Nonnull
        private final List<Pattern> subPatterns;

        public Leaf(@Nonnull List<Pattern> subPatterns) {
            this.subPatterns = subPatterns;
        }

        @Nonnull
        public List<Pattern> getSubPatterns() {
            return subPatterns;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof Leaf)) return false;
            return subPatterns.equals(((Leaf) o).subPatterns);
        }

        @Override
        public int hashCode() {
            return subPatterns.hashCode();
        }

        @Override
        public String toString() {
            return "Leaf(subPatterns=" + subPatterns + ")";
        }
    }

    /** &P, &mut P, etc */
    public static final class Deref extends PatternKind {
        @Nonnull
        private final Pattern subPattern;

        public Deref(@Nonnull Pattern subPattern) {
            this.subPattern = subPattern;
        }

        @Nonnull
        public Pattern getSubPattern() {
            return subPattern;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof Deref)) return false;
            return subPattern.equals(((Deref) o).subPattern);
        }

        @Override
        public int hashCode() {
            return subPattern.hashCode();
        }

        @Override
        public String toString() {
            return "Deref(subPattern=" + subPattern + ")";
        }
    }

    public static final class Const extends PatternKind {
        @Nonnull
        private final ConstExpr.Value<?> value;

        public Const(@Nonnull ConstExpr.Value<?> value) {
            this.value = value;
        }

        @Nonnull
        public ConstExpr.Value<?> getValue() {
            return value;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof Const)) return false;
            return value.equals(((Const) o).value);
        }

        @Override
        public int hashCode() {
            return value.hashCode();
        }

        @Override
        public String toString() {
            return "Const(value=" + value + ")";
        }
    }

    public static final class Range extends PatternKind {
        @Nonnull
        private final ConstExpr.Value<?> lc;
        @Nonnull
        private final ConstExpr.Value<?> rc;
        private final boolean isInclusive;

        public Range(@Nonnull ConstExpr.Value<?> lc, @Nonnull ConstExpr.Value<?> rc, boolean isInclusive) {
            this.lc = lc;
            this.rc = rc;
            this.isInclusive = isInclusive;
        }

        @Nonnull
        public ConstExpr.Value<?> getLc() {
            return lc;
        }

        @Nonnull
        public ConstExpr.Value<?> getRc() {
            return rc;
        }

        public boolean isInclusive() {
            return isInclusive;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof Range)) return false;
            Range range = (Range) o;
            return isInclusive == range.isInclusive && lc.equals(range.lc) && rc.equals(range.rc);
        }

        @Override
        public int hashCode() {
            return Objects.hash(lc, rc, isInclusive);
        }

        @Override
        public String toString() {
            return "Range(lc=" + lc + ", rc=" + rc + ", isInclusive=" + isInclusive + ")";
        }
    }

    public interface SliceField {
        @Nonnull
        List<Pattern> getPrefix();

        @Nullable
        Pattern getSlice();

        @Nonnull
        List<Pattern> getSuffix();
    }

    /**
     * Matches against a slice, checking the length and extracting elements.
     * Irrefutable when there is a slice pattern and both {@code prefix} and {@code suffix} are empty
     * e.g. {@code &[ref xs..]}
     */
    public static final class Slice extends PatternKind implements SliceField {
        @Nonnull
        private final List<Pattern> prefix;
        @Nullable
        private final Pattern slice;
        @Nonnull
        private final List<Pattern> suffix;

        public Slice(@Nonnull List<Pattern> prefix, @Nullable Pattern slice, @Nonnull List<Pattern> suffix) {
            this.prefix = prefix;
            this.slice = slice;
            this.suffix = suffix;
        }

        @Override
        @Nonnull
        public List<Pattern> getPrefix() {
            return prefix;
        }

        @Override
        @Nullable
        public Pattern getSlice() {
            return slice;
        }

        @Override
        @Nonnull
        public List<Pattern> getSuffix() {
            return suffix;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof Slice)) return false;
            Slice s = (Slice) o;
            return prefix.equals(s.prefix) && Objects.equals(slice, s.slice) && suffix.equals(s.suffix);
        }

        @Override
        public int hashCode() {
            return Objects.hash(prefix, slice, suffix);
        }

        @Override
        public String toString() {
            return "Slice(prefix=" + prefix + ", slice=" + slice + ", suffix=" + suffix + ")";
        }
    }

    /** Fixed match against an array, irrefutable */
    public static final class Array extends PatternKind implements SliceField {
        @Nonnull
        private final List<Pattern> prefix;
        @Nullable
        private final Pattern slice;
        @Nonnull
        private final List<Pattern> suffix;

        public Array(@Nonnull List<Pattern> prefix, @Nullable Pattern slice, @Nonnull List<Pattern> suffix) {
            this.prefix = prefix;
            this.slice = slice;
            this.suffix = suffix;
        }

        @Override
        @Nonnull
        public List<Pattern> getPrefix() {
            return prefix;
        }

        @Override
        @Nullable
        public Pattern getSlice() {
            return slice;
        }

        @Override
        @Nonnull
        public List<Pattern> getSuffix() {
            return suffix;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof Array)) return false;
            Array a = (Array) o;
            return prefix.equals(a.prefix) && Objects.equals(slice, a.slice) && suffix.equals(a.suffix);
        }

        @Override
        public int hashCode() {
            return Objects.hash(prefix, slice, suffix);
        }

        @Override
        public String toString() {
            return "Array(prefix=" + prefix + ", slice=" + slice + ", suffix=" + suffix + ")";
        }
    }
}
