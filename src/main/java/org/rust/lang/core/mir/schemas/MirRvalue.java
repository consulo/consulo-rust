/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.mir.schemas;

import org.jetbrains.annotations.NotNull;
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
        @NotNull
        private final MirOperand operand;

        public Use(@NotNull MirOperand operand) {
            this.operand = operand;
        }

        @NotNull
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
        @NotNull
        private final UnaryOperator op;
        @NotNull
        private final MirOperand operand;

        public UnaryOpUse(@NotNull UnaryOperator op, @NotNull MirOperand operand) {
            this.op = op;
            this.operand = operand;
        }

        @NotNull
        public UnaryOperator getOp() {
            return op;
        }

        @NotNull
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
        @NotNull
        private final MirBinaryOperator op;
        @NotNull
        private final MirOperand left;
        @NotNull
        private final MirOperand right;

        public BinaryOpUse(@NotNull MirBinaryOperator op, @NotNull MirOperand left, @NotNull MirOperand right) {
            this.op = op;
            this.left = left;
            this.right = right;
        }

        @NotNull
        public MirBinaryOperator getOp() {
            return op;
        }

        @NotNull
        public MirOperand getLeft() {
            return left;
        }

        @NotNull
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
        @NotNull
        private final MirBinaryOperator op;
        @NotNull
        private final MirOperand left;
        @NotNull
        private final MirOperand right;

        public CheckedBinaryOpUse(@NotNull MirBinaryOperator op, @NotNull MirOperand left, @NotNull MirOperand right) {
            this.op = op;
            this.left = left;
            this.right = right;
        }

        @NotNull
        public MirBinaryOperator getOp() {
            return op;
        }

        @NotNull
        public MirOperand getLeft() {
            return left;
        }

        @NotNull
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
        @NotNull
        private final List<MirOperand> operands;

        protected Aggregate(@NotNull List<MirOperand> operands) {
            this.operands = operands;
        }

        @NotNull
        public List<MirOperand> getOperands() {
            return operands;
        }

        public static final class Array extends Aggregate {
            @NotNull
            private final Ty ty;

            public Array(@NotNull Ty ty, @NotNull List<MirOperand> operands) {
                super(operands);
                this.ty = ty;
            }

            @NotNull
            public Ty getTy() {
                return ty;
            }
        }

        public static final class Tuple extends Aggregate {
            public Tuple(@NotNull List<MirOperand> operands) {
                super(operands);
            }
        }

        public static final class Adt extends Aggregate {
            @NotNull
            private final RsStructOrEnumItemElement definition;
            private final int variantIndex;
            @NotNull
            private final Ty ty;

            public Adt(
                @NotNull RsStructOrEnumItemElement definition,
                int variantIndex,
                @NotNull Ty ty,
                @NotNull List<MirOperand> operands
            ) {
                super(operands);
                this.definition = definition;
                this.variantIndex = variantIndex;
                this.ty = ty;
            }

            @NotNull
            public RsStructOrEnumItemElement getDefinition() {
                return definition;
            }

            public int getVariantIndex() {
                return variantIndex;
            }

            @NotNull
            public Ty getTy() {
                return ty;
            }
        }
    }

    public static final class Repeat extends MirRvalue {
        @NotNull
        private final MirOperand operand;
        @NotNull
        private final Const count;

        public Repeat(@NotNull MirOperand operand, @NotNull Const count) {
            this.operand = operand;
            this.count = count;
        }

        @NotNull
        public MirOperand getOperand() {
            return operand;
        }

        @NotNull
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
        @NotNull
        private final MirBorrowKind borrowKind;
        @NotNull
        private final MirPlace place;

        public Ref(@NotNull MirBorrowKind borrowKind, @NotNull MirPlace place) {
            this.borrowKind = borrowKind;
            this.place = place;
        }

        @NotNull
        public MirBorrowKind getBorrowKind() {
            return borrowKind;
        }

        @NotNull
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
        @NotNull
        private final MirPlace place;

        public Len(@NotNull MirPlace place) {
            this.place = place;
        }

        @NotNull
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
        @NotNull
        private final MirPlace place;

        public Discriminant(@NotNull MirPlace place) {
            this.place = place;
        }

        @NotNull
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
        @NotNull
        private final MirOperand operand;
        @NotNull
        private final Ty ty;

        protected Cast(@NotNull MirOperand operand, @NotNull Ty ty) {
            this.operand = operand;
            this.ty = ty;
        }

        @NotNull
        public MirOperand getOperand() {
            return operand;
        }

        @NotNull
        public Ty getTy() {
            return ty;
        }

        public static final class IntToInt extends Cast {
            public IntToInt(@NotNull MirOperand operand, @NotNull Ty ty) {
                super(operand, ty);
            }
        }
        // TODO: there are a lot more of possible casts
    }
}
