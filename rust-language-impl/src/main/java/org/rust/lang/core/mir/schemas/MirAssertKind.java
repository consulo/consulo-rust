/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.mir.schemas;

import jakarta.annotation.Nonnull;
import org.rust.lang.core.psi.ext.impl.ArithmeticOp;

import java.util.Objects;

public abstract class MirAssertKind {
    private MirAssertKind() {
    }

    public static final class BoundsCheck extends MirAssertKind {
        @Nonnull
        private final MirOperand len;
        @Nonnull
        private final MirOperand index;

        public BoundsCheck(@Nonnull MirOperand len, @Nonnull MirOperand index) {
            this.len = len;
            this.index = index;
        }

        @Nonnull
        public MirOperand getLen() {
            return len;
        }

        @Nonnull
        public MirOperand getIndex() {
            return index;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            BoundsCheck that = (BoundsCheck) o;
            return Objects.equals(len, that.len) && Objects.equals(index, that.index);
        }

        @Override
        public int hashCode() {
            return Objects.hash(len, index);
        }

        @Override
        public String toString() {
            return "BoundsCheck(len=" + len + ", index=" + index + ")";
        }
    }

    public static final class OverflowNeg extends MirAssertKind {
        @Nonnull
        private final MirOperand arg;

        public OverflowNeg(@Nonnull MirOperand arg) {
            this.arg = arg;
        }

        @Nonnull
        public MirOperand getArg() {
            return arg;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            OverflowNeg that = (OverflowNeg) o;
            return Objects.equals(arg, that.arg);
        }

        @Override
        public int hashCode() {
            return Objects.hash(arg);
        }

        @Override
        public String toString() {
            return "OverflowNeg(arg=" + arg + ")";
        }
    }

    public static final class Overflow extends MirAssertKind {
        @Nonnull
        private final ArithmeticOp op;
        @Nonnull
        private final MirOperand left;
        @Nonnull
        private final MirOperand right;

        public Overflow(@Nonnull ArithmeticOp op, @Nonnull MirOperand left, @Nonnull MirOperand right) {
            this.op = op;
            this.left = left;
            this.right = right;
        }

        @Nonnull
        public ArithmeticOp getOp() {
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
            Overflow that = (Overflow) o;
            return Objects.equals(op, that.op) && Objects.equals(left, that.left) && Objects.equals(right, that.right);
        }

        @Override
        public int hashCode() {
            return Objects.hash(op, left, right);
        }

        @Override
        public String toString() {
            return "Overflow(op=" + op + ", left=" + left + ", right=" + right + ")";
        }
    }

    public static final class DivisionByZero extends MirAssertKind {
        @Nonnull
        private final MirOperand arg;

        public DivisionByZero(@Nonnull MirOperand arg) {
            this.arg = arg;
        }

        @Nonnull
        public MirOperand getArg() {
            return arg;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            DivisionByZero that = (DivisionByZero) o;
            return Objects.equals(arg, that.arg);
        }

        @Override
        public int hashCode() {
            return Objects.hash(arg);
        }

        @Override
        public String toString() {
            return "DivisionByZero(arg=" + arg + ")";
        }
    }

    public static final class ReminderByZero extends MirAssertKind {
        @Nonnull
        private final MirOperand arg;

        public ReminderByZero(@Nonnull MirOperand arg) {
            this.arg = arg;
        }

        @Nonnull
        public MirOperand getArg() {
            return arg;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            ReminderByZero that = (ReminderByZero) o;
            return Objects.equals(arg, that.arg);
        }

        @Override
        public int hashCode() {
            return Objects.hash(arg);
        }

        @Override
        public String toString() {
            return "ReminderByZero(arg=" + arg + ")";
        }
    }
}
