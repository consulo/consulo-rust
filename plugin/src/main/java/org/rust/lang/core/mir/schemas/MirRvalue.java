/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.mir.schemas;

import jakarta.annotation.Nonnull;
import org.rust.lang.core.psi.ext.RsStructOrEnumItemElement;
import org.rust.lang.core.psi.ext.UnaryOperator;
import org.rust.lang.core.types.consts.Const;
import org.rust.lang.core.types.ty.Ty;

import java.util.List;
import java.util.Objects;

public abstract class MirRvalue {
    private MirRvalue() {
    }

    public static final class Use extends MirRvalue {
        @Nonnull
        private final MirOperand operand;

        public Use(@Nonnull MirOperand operand) {
            this.operand = operand;
        }

        @Nonnull
        public MirOperand getOperand() {
            return operand;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Use use = (Use) o;
            return Objects.equals(operand, use.operand);
        }

        @Override
        public int hashCode() {
            return Objects.hash(operand);
        }

        @Override
        public String toString() {
            return "Use(operand=" + operand + ")";
        }
    }

    public static final class UnaryOpUse extends MirRvalue {
        @Nonnull
        private final UnaryOperator op;
        @Nonnull
        private final MirOperand operand;

        public UnaryOpUse(@Nonnull UnaryOperator op, @Nonnull MirOperand operand) {
            this.op = op;
            this.operand = operand;
        }

        @Nonnull
        public UnaryOperator getOp() {
            return op;
        }

        @Nonnull
        public MirOperand getOperand() {
            return operand;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            UnaryOpUse that = (UnaryOpUse) o;
            return Objects.equals(op, that.op) && Objects.equals(operand, that.operand);
        }

        @Override
        public int hashCode() {
            return Objects.hash(op, operand);
        }

        @Override
        public String toString() {
            return "UnaryOpUse(op=" + op + ", operand=" + operand + ")";
        }
    }

    public static final class BinaryOpUse extends MirRvalue {
        @Nonnull
        private final MirBinaryOperator op;
        @Nonnull
        private final MirOperand left;
        @Nonnull
        private final MirOperand right;

        public BinaryOpUse(@Nonnull MirBinaryOperator op, @Nonnull MirOperand left, @Nonnull MirOperand right) {
            this.op = op;
            this.left = left;
            this.right = right;
        }

        @Nonnull
        public MirBinaryOperator getOp() {
            return op;
        }

        @Nonnull
        public MirOperand getLeft() {
            return left;
        }

        @Nonnull
        public MirOperand getRight() {
            return right;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            BinaryOpUse that = (BinaryOpUse) o;
            return Objects.equals(op, that.op) && Objects.equals(left, that.left) && Objects.equals(right, that.right);
        }

        @Override
        public int hashCode() {
            return Objects.hash(op, left, right);
        }

        @Override
        public String toString() {
            return "BinaryOpUse(op=" + op + ", left=" + left + ", right=" + right + ")";
        }
    }

    public static final class CheckedBinaryOpUse extends MirRvalue {
        @Nonnull
        private final MirBinaryOperator op;
        @Nonnull
        private final MirOperand left;
        @Nonnull
        private final MirOperand right;

        public CheckedBinaryOpUse(@Nonnull MirBinaryOperator op, @Nonnull MirOperand left, @Nonnull MirOperand right) {
            this.op = op;
            this.left = left;
            this.right = right;
        }

        @Nonnull
        public MirBinaryOperator getOp() {
            return op;
        }

        @Nonnull
        public MirOperand getLeft() {
            return left;
        }

        @Nonnull
        public MirOperand getRight() {
            return right;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            CheckedBinaryOpUse that = (CheckedBinaryOpUse) o;
            return Objects.equals(op, that.op) && Objects.equals(left, that.left) && Objects.equals(right, that.right);
        }

        @Override
        public int hashCode() {
            return Objects.hash(op, left, right);
        }

        @Override
        public String toString() {
            return "CheckedBinaryOpUse(op=" + op + ", left=" + left + ", right=" + right + ")";
        }
    }

    public static final class NullaryOpUse extends MirRvalue {
        public static final NullaryOpUse INSTANCE = new NullaryOpUse();

        private NullaryOpUse() {
        }

        @Override
        public String toString() {
            return "NullaryOpUse";
        }
    }

    public abstract static class Aggregate extends MirRvalue {
        @Nonnull
        private final List<MirOperand> operands;

        protected Aggregate(@Nonnull List<MirOperand> operands) {
            this.operands = operands;
        }

        @Nonnull
        public List<MirOperand> getOperands() {
            return operands;
        }

        public static final class Array extends Aggregate {
            @Nonnull
            private final Ty ty;

            public Array(@Nonnull Ty ty, @Nonnull List<MirOperand> operands) {
                super(operands);
                this.ty = ty;
            }

            @Nonnull
            public Ty getTy() {
                return ty;
            }
        }

        public static final class Tuple extends Aggregate {
            public Tuple(@Nonnull List<MirOperand> operands) {
                super(operands);
            }
        }

        public static final class Adt extends Aggregate {
            @Nonnull
            private final RsStructOrEnumItemElement definition;
            private final int variantIndex;
            @Nonnull
            private final Ty ty;

            public Adt(
                @Nonnull RsStructOrEnumItemElement definition,
                int variantIndex,
                @Nonnull Ty ty,
                @Nonnull List<MirOperand> operands
            ) {
                super(operands);
                this.definition = definition;
                this.variantIndex = variantIndex;
                this.ty = ty;
            }

            @Nonnull
            public RsStructOrEnumItemElement getDefinition() {
                return definition;
            }

            public int getVariantIndex() {
                return variantIndex;
            }

            @Nonnull
            public Ty getTy() {
                return ty;
            }
        }
    }

    public static final class Repeat extends MirRvalue {
        @Nonnull
        private final MirOperand operand;
        @Nonnull
        private final Const count;

        public Repeat(@Nonnull MirOperand operand, @Nonnull Const count) {
            this.operand = operand;
            this.count = count;
        }

        @Nonnull
        public MirOperand getOperand() {
            return operand;
        }

        @Nonnull
        public Const getCount() {
            return count;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Repeat repeat = (Repeat) o;
            return Objects.equals(operand, repeat.operand) && Objects.equals(count, repeat.count);
        }

        @Override
        public int hashCode() {
            return Objects.hash(operand, count);
        }

        @Override
        public String toString() {
            return "Repeat(operand=" + operand + ", count=" + count + ")";
        }
    }

    public static final class Ref extends MirRvalue {
        @Nonnull
        private final MirBorrowKind borrowKind;
        @Nonnull
        private final MirPlace place;

        public Ref(@Nonnull MirBorrowKind borrowKind, @Nonnull MirPlace place) {
            this.borrowKind = borrowKind;
            this.place = place;
        }

        @Nonnull
        public MirBorrowKind getBorrowKind() {
            return borrowKind;
        }

        @Nonnull
        public MirPlace getPlace() {
            return place;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Ref ref = (Ref) o;
            return Objects.equals(borrowKind, ref.borrowKind) && Objects.equals(place, ref.place);
        }

        @Override
        public int hashCode() {
            return Objects.hash(borrowKind, place);
        }

        @Override
        public String toString() {
            return "Ref(borrowKind=" + borrowKind + ", place=" + place + ")";
        }
    }

    public static final class Len extends MirRvalue {
        @Nonnull
        private final MirPlace place;

        public Len(@Nonnull MirPlace place) {
            this.place = place;
        }

        @Nonnull
        public MirPlace getPlace() {
            return place;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Len len = (Len) o;
            return Objects.equals(place, len.place);
        }

        @Override
        public int hashCode() {
            return Objects.hash(place);
        }

        @Override
        public String toString() {
            return "Len(place=" + place + ")";
        }
    }

    public static final class ThreadLocalRef extends MirRvalue {
        public static final ThreadLocalRef INSTANCE = new ThreadLocalRef();

        private ThreadLocalRef() {
        }

        @Override
        public String toString() {
            return "ThreadLocalRef";
        }
    }

    public static final class AddressOf extends MirRvalue {
        public static final AddressOf INSTANCE = new AddressOf();

        private AddressOf() {
        }

        @Override
        public String toString() {
            return "AddressOf";
        }
    }

    public static final class Discriminant extends MirRvalue {
        @Nonnull
        private final MirPlace place;

        public Discriminant(@Nonnull MirPlace place) {
            this.place = place;
        }

        @Nonnull
        public MirPlace getPlace() {
            return place;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Discriminant that = (Discriminant) o;
            return Objects.equals(place, that.place);
        }

        @Override
        public int hashCode() {
            return Objects.hash(place);
        }

        @Override
        public String toString() {
            return "Discriminant(place=" + place + ")";
        }
    }

    public static final class CopyForDeref extends MirRvalue {
        public static final CopyForDeref INSTANCE = new CopyForDeref();

        private CopyForDeref() {
        }

        @Override
        public String toString() {
            return "CopyForDeref";
        }
    }

    public abstract static class Cast extends MirRvalue {
        @Nonnull
        private final MirOperand operand;
        @Nonnull
        private final Ty ty;

        protected Cast(@Nonnull MirOperand operand, @Nonnull Ty ty) {
            this.operand = operand;
            this.ty = ty;
        }

        @Nonnull
        public MirOperand getOperand() {
            return operand;
        }

        @Nonnull
        public Ty getTy() {
            return ty;
        }

        public static final class IntToInt extends Cast {
            public IntToInt(@Nonnull MirOperand operand, @Nonnull Ty ty) {
                super(operand, ty);
            }
        }
        // TODO: there are a lot more of possible casts
    }
}
