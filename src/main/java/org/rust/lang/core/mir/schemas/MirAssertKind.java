/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.mir.schemas;

import org.jetbrains.annotations.NotNull;
import org.rust.lang.core.psi.ext.ArithmeticOp;

import java.util.Objects;

public abstract class MirAssertKind {
    private MirAssertKind() {
    }

    public static final class BoundsCheck extends MirAssertKind {
        @NotNull
        private final MirOperand len;
        @NotNull
        private final MirOperand index;

        public BoundsCheck(@NotNull MirOperand len, @NotNull MirOperand index) {
            this.len = len;
            this.index = index;
        }

        @NotNull
        public MirOperand getLen() {
            return len;
        }

        @NotNull
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
        @NotNull
        private final MirOperand arg;

        public OverflowNeg(@NotNull MirOperand arg) {
            this.arg = arg;
        }

        @NotNull
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
        @NotNull
        private final ArithmeticOp op;
        @NotNull
        private final MirOperand left;
        @NotNull
        private final MirOperand right;

        public Overflow(@NotNull ArithmeticOp op, @NotNull MirOperand left, @NotNull MirOperand right) {
            this.op = op;
            this.left = left;
            this.right = right;
        }

        @NotNull
        public ArithmeticOp getOp() {
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
        @NotNull
        private final MirOperand arg;

        public DivisionByZero(@NotNull MirOperand arg) {
            this.arg = arg;
        }

        @NotNull
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
        @NotNull
        private final MirOperand arg;

        public ReminderByZero(@NotNull MirOperand arg) {
            this.arg = arg;
        }

        @NotNull
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
