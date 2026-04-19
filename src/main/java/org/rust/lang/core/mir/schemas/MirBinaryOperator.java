/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.mir.schemas;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.rust.lang.core.psi.ext.ArithmeticOp;
import org.rust.lang.core.psi.ext.BinaryOperator;
import org.rust.lang.core.psi.ext.ComparisonOp;
import org.rust.lang.core.psi.ext.EqualityOp;

import java.util.Objects;

public interface MirBinaryOperator {
    @Nullable
    BinaryOperator getUnderlyingOp();

    final class Arithmetic implements MirBinaryOperator {
        @NotNull
        private final ArithmeticOp op;

        public Arithmetic(@NotNull ArithmeticOp op) {
            this.op = op;
        }

        @NotNull
        public ArithmeticOp getOp() {
            return op;
        }

        @Override
        @NotNull
        public BinaryOperator getUnderlyingOp() {
            return op;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Arithmetic that = (Arithmetic) o;
            return Objects.equals(op, that.op);
        }

        @Override
        public int hashCode() {
            return Objects.hash(op);
        }

        @Override
        public String toString() {
            return "Arithmetic(op=" + op + ")";
        }
    }

    final class Equality implements MirBinaryOperator {
        @NotNull
        private final EqualityOp op;

        public Equality(@NotNull EqualityOp op) {
            this.op = op;
        }

        @NotNull
        public EqualityOp getOp() {
            return op;
        }

        @Override
        @NotNull
        public BinaryOperator getUnderlyingOp() {
            return op;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Equality that = (Equality) o;
            return Objects.equals(op, that.op);
        }

        @Override
        public int hashCode() {
            return Objects.hash(op);
        }

        @Override
        public String toString() {
            return "Equality(op=" + op + ")";
        }
    }

    final class Comparison implements MirBinaryOperator {
        @NotNull
        private final ComparisonOp op;

        public Comparison(@NotNull ComparisonOp op) {
            this.op = op;
        }

        @NotNull
        public ComparisonOp getOp() {
            return op;
        }

        @Override
        @NotNull
        public BinaryOperator getUnderlyingOp() {
            return op;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Comparison that = (Comparison) o;
            return Objects.equals(op, that.op);
        }

        @Override
        public int hashCode() {
            return Objects.hash(op);
        }

        @Override
        public String toString() {
            return "Comparison(op=" + op + ")";
        }
    }

    final class Offset implements MirBinaryOperator {
        public static final Offset INSTANCE = new Offset();

        private Offset() {
        }

        @Override
        @Nullable
        public BinaryOperator getUnderlyingOp() {
            return null;
        }

        @Override
        public String toString() {
            return "Offset";
        }
    }

    /**
     * Converts a BinaryOperator to its MIR equivalent.
     */
    @NotNull
    static MirBinaryOperator toMir(@NotNull BinaryOperator op) {
        if (op instanceof ArithmeticOp) {
            return new Arithmetic((ArithmeticOp) op);
        } else if (op instanceof EqualityOp) {
            return new Equality((EqualityOp) op);
        } else if (op instanceof ComparisonOp) {
            return new Comparison((ComparisonOp) op);
        } else {
            throw new IllegalStateException(op + " cannot be a mir operator");
        }
    }
}
