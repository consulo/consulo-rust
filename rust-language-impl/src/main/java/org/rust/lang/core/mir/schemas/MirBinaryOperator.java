/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.mir.schemas;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.rust.lang.core.psi.ext.impl.ArithmeticOp;
import org.rust.lang.core.psi.ext.impl.BinaryOperator;
import org.rust.lang.core.psi.ext.impl.ComparisonOp;
import org.rust.lang.core.psi.ext.impl.EqualityOp;

import java.util.Objects;

public interface MirBinaryOperator {
    @Nullable
    BinaryOperator getUnderlyingOp();

    final class Arithmetic implements MirBinaryOperator {
        @Nonnull
        private final ArithmeticOp op;

        public Arithmetic(@Nonnull ArithmeticOp op) {
            this.op = op;
        }

        @Nonnull
        public ArithmeticOp getOp() {
            return op;
        }

        @Override
        @Nonnull
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
        @Nonnull
        private final EqualityOp op;

        public Equality(@Nonnull EqualityOp op) {
            this.op = op;
        }

        @Nonnull
        public EqualityOp getOp() {
            return op;
        }

        @Override
        @Nonnull
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
        @Nonnull
        private final ComparisonOp op;

        public Comparison(@Nonnull ComparisonOp op) {
            this.op = op;
        }

        @Nonnull
        public ComparisonOp getOp() {
            return op;
        }

        @Override
        @Nonnull
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
    @Nonnull
    static MirBinaryOperator toMir(@Nonnull BinaryOperator op) {
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
