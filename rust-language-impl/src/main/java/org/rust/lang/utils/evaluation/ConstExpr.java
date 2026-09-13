/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.utils.evaluation;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.rust.lang.core.psi.ext.impl.BinaryOperator;
import org.rust.lang.core.psi.ext.impl.UnaryOperator;
// import removed - placeholder
import org.rust.lang.core.types.consts.Const;
import org.rust.lang.core.types.consts.CtUnevaluated;
import org.rust.lang.core.types.consts.CtUnknown;
import org.rust.lang.core.types.consts.CtValue;
import org.rust.lang.core.types.infer.TypeFoldable;
import org.rust.lang.core.types.infer.TypeFolder;
import org.rust.lang.core.types.infer.TypeVisitor;
import org.rust.lang.core.types.ty.*;

import java.util.Objects;

public abstract class ConstExpr<T extends Ty> implements TypeFoldable<ConstExpr<T>> {
    private final int myFlags;

    protected ConstExpr() {
        this(0);
    }

    protected ConstExpr(int flags) {
        myFlags = flags;
    }

    public int getFlags() {
        return myFlags;
    }

    @Nullable
    public abstract T getExpectedTy();

    @Nonnull
    @SuppressWarnings("unchecked")
    public static Const toConst(@Nonnull ConstExpr<?> expr) {
        if (expr instanceof Constant) {
            return ((Constant<?>) expr).myConst;
        }
        if (expr instanceof Value) {
            return new CtValue((Value<? extends Ty>) expr);
        }
        if (expr instanceof Error) {
            return CtUnknown.INSTANCE;
        }
        return new CtUnevaluated(expr);
    }

    // ---- Subclasses ----

    public static class Unary<T extends Ty> extends ConstExpr<T> {
        @Nonnull
        private final UnaryOperator myOperator;
        @Nonnull
        private final ConstExpr<T> myExpr;
        @Nonnull
        private final T myExpectedTy;

        public Unary(@Nonnull UnaryOperator operator, @Nonnull ConstExpr<T> expr, @Nonnull T expectedTy) {
            super(expr.getFlags());
            myOperator = operator;
            myExpr = expr;
            myExpectedTy = expectedTy;
        }

        @Nonnull
        public UnaryOperator getOperator() {
            return myOperator;
        }

        @Nonnull
        public ConstExpr<T> getExpr() {
            return myExpr;
        }

        @Nonnull
        @Override
        public T getExpectedTy() {
            return myExpectedTy;
        }

        @Nonnull
        @Override
        public Unary<T> superFoldWith(@Nonnull TypeFolder folder) {
            return new Unary<>(myOperator, myExpr.foldWith(folder), myExpectedTy);
        }

        @Override
        public boolean superVisitWith(@Nonnull TypeVisitor visitor) {
            return myExpr.visitWith(visitor);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Unary<?> unary = (Unary<?>) o;
            return Objects.equals(myOperator, unary.myOperator) &&
                Objects.equals(myExpr, unary.myExpr) &&
                Objects.equals(myExpectedTy, unary.myExpectedTy);
        }

        @Override
        public int hashCode() {
            return Objects.hash(myOperator, myExpr, myExpectedTy);
        }
    }

    public static class Binary<T extends Ty> extends ConstExpr<T> {
        @Nonnull
        private final ConstExpr<T> myLeft;
        @Nonnull
        private final BinaryOperator myOperator;
        @Nonnull
        private final ConstExpr<T> myRight;
        @Nonnull
        private final T myExpectedTy;

        public Binary(@Nonnull ConstExpr<T> left, @Nonnull BinaryOperator operator,
                       @Nonnull ConstExpr<T> right, @Nonnull T expectedTy) {
            super(left.getFlags() | right.getFlags());
            myLeft = left;
            myOperator = operator;
            myRight = right;
            myExpectedTy = expectedTy;
        }

        @Nonnull
        public ConstExpr<T> getLeft() {
            return myLeft;
        }

        @Nonnull
        public BinaryOperator getOperator() {
            return myOperator;
        }

        @Nonnull
        public ConstExpr<T> getRight() {
            return myRight;
        }

        @Nonnull
        @Override
        public T getExpectedTy() {
            return myExpectedTy;
        }

        @Nonnull
        @Override
        public Binary<T> superFoldWith(@Nonnull TypeFolder folder) {
            return new Binary<>(myLeft.foldWith(folder), myOperator, myRight.foldWith(folder), myExpectedTy);
        }

        @Override
        public boolean superVisitWith(@Nonnull TypeVisitor visitor) {
            return myLeft.visitWith(visitor) || myRight.visitWith(visitor);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Binary<?> binary = (Binary<?>) o;
            return Objects.equals(myLeft, binary.myLeft) &&
                Objects.equals(myOperator, binary.myOperator) &&
                Objects.equals(myRight, binary.myRight) &&
                Objects.equals(myExpectedTy, binary.myExpectedTy);
        }

        @Override
        public int hashCode() {
            return Objects.hash(myLeft, myOperator, myRight, myExpectedTy);
        }
    }

    public static class Constant<T extends Ty> extends ConstExpr<T> {
        @Nonnull
        final Const myConst;
        @Nonnull
        private final T myExpectedTy;

        public Constant(@Nonnull Const constValue, @Nonnull T expectedTy) {
            super(constValue.getFlags());
            myConst = constValue;
            myExpectedTy = expectedTy;
        }

        @Nonnull
        public Const getConst() {
            return myConst;
        }

        @Nonnull
        @Override
        public T getExpectedTy() {
            return myExpectedTy;
        }

        @Nonnull
        @Override
        public Constant<T> superFoldWith(@Nonnull TypeFolder folder) {
            return new Constant<>(myConst.foldWith(folder), myExpectedTy);
        }

        @Override
        public boolean superVisitWith(@Nonnull TypeVisitor visitor) {
            return myConst.visitWith(visitor);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Constant<?> constant = (Constant<?>) o;
            return Objects.equals(myConst, constant.myConst) &&
                Objects.equals(myExpectedTy, constant.myExpectedTy);
        }

        @Override
        public int hashCode() {
            return Objects.hash(myConst, myExpectedTy);
        }
    }

    public static abstract class Value<T extends Ty> extends ConstExpr<T> {
        @Nonnull
        @Override
        public Value<T> superFoldWith(@Nonnull TypeFolder folder) {
            return this;
        }

        @Override
        public boolean superVisitWith(@Nonnull TypeVisitor visitor) {
            return false;
        }

        public static class Bool extends Value<TyBool> {
            private final boolean myValue;

            public Bool(boolean value) {
                myValue = value;
            }

            public boolean getValue() {
                return myValue;
            }

            @Nonnull
            @Override
            public TyBool getExpectedTy() {
                return TyBool.INSTANCE;
            }

            @Override
            public String toString() {
                return String.valueOf(myValue);
            }

            @Override
            public boolean equals(Object o) {
                if (this == o) return true;
                if (o == null || getClass() != o.getClass()) return false;
                Bool bool = (Bool) o;
                return myValue == bool.myValue;
            }

            @Override
            public int hashCode() {
                return Objects.hash(myValue);
            }
        }

        public static class Integer extends Value<TyInteger> {
            private final long myValue;
            @Nonnull
            private final TyInteger myExpectedTy;

            public Integer(long value, @Nonnull TyInteger expectedTy) {
                myValue = value;
                myExpectedTy = expectedTy;
            }

            public long getValue() {
                return myValue;
            }

            @Nonnull
            @Override
            public TyInteger getExpectedTy() {
                return myExpectedTy;
            }

            @Override
            public String toString() {
                return String.valueOf(myValue);
            }

            @Override
            public boolean equals(Object o) {
                if (this == o) return true;
                if (o == null || getClass() != o.getClass()) return false;
                Integer integer = (Integer) o;
                return myValue == integer.myValue && Objects.equals(myExpectedTy, integer.myExpectedTy);
            }

            @Override
            public int hashCode() {
                return Objects.hash(myValue, myExpectedTy);
            }
        }

        public static class Float extends Value<TyFloat> {
            private final double myValue;
            @Nonnull
            private final TyFloat myExpectedTy;

            public Float(double value, @Nonnull TyFloat expectedTy) {
                myValue = value;
                myExpectedTy = expectedTy;
            }

            public double getValue() {
                return myValue;
            }

            @Nonnull
            @Override
            public TyFloat getExpectedTy() {
                return myExpectedTy;
            }

            @Override
            public String toString() {
                return String.valueOf(myValue);
            }

            @Override
            public boolean equals(Object o) {
                if (this == o) return true;
                if (o == null || getClass() != o.getClass()) return false;
                Float aFloat = (Float) o;
                return Double.compare(aFloat.myValue, myValue) == 0 &&
                    Objects.equals(myExpectedTy, aFloat.myExpectedTy);
            }

            @Override
            public int hashCode() {
                return Objects.hash(myValue, myExpectedTy);
            }
        }

        public static class Char extends Value<TyChar> {
            @Nonnull
            private final String myValue;

            public Char(@Nonnull String value) {
                myValue = value;
            }

            @Nonnull
            public String getValue() {
                return myValue;
            }

            @Nonnull
            @Override
            public TyChar getExpectedTy() {
                return TyChar.INSTANCE;
            }

            @Override
            public String toString() {
                return myValue;
            }

            @Override
            public boolean equals(Object o) {
                if (this == o) return true;
                if (o == null || getClass() != o.getClass()) return false;
                Char aChar = (Char) o;
                return Objects.equals(myValue, aChar.myValue);
            }

            @Override
            public int hashCode() {
                return Objects.hash(myValue);
            }
        }

        public static class Str extends Value<TyReference> {
            @Nonnull
            private final String myValue;
            @Nonnull
            private final TyReference myExpectedTy;

            public Str(@Nonnull String value, @Nonnull TyReference expectedTy) {
                myValue = value;
                myExpectedTy = expectedTy;
            }

            @Nonnull
            public String getValue() {
                return myValue;
            }

            @Nonnull
            @Override
            public TyReference getExpectedTy() {
                return myExpectedTy;
            }

            @Override
            public String toString() {
                return myValue;
            }

            @Override
            public boolean equals(Object o) {
                if (this == o) return true;
                if (o == null || getClass() != o.getClass()) return false;
                Str str = (Str) o;
                return Objects.equals(myValue, str.myValue) &&
                    Objects.equals(myExpectedTy, str.myExpectedTy);
            }

            @Override
            public int hashCode() {
                return Objects.hash(myValue, myExpectedTy);
            }
        }
    }

    public static class Error<T extends Ty> extends ConstExpr<T> {
        @Nullable
        @Override
        public T getExpectedTy() {
            return null;
        }

        @Nonnull
        @Override
        public ConstExpr<T> superFoldWith(@Nonnull TypeFolder folder) {
            return this;
        }

        @Override
        public boolean superVisitWith(@Nonnull TypeVisitor visitor) {
            return false;
        }
    }
}
