/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.mir.schemas;

import org.jetbrains.annotations.NotNull;

import java.util.Objects;

public abstract class MirOperand {
    private MirOperand() {
    }

    @NotNull
    public abstract MirOperand toCopy();

    public static final class Constant extends MirOperand {
        @NotNull
        private final MirConstant constant;

        public Constant(@NotNull MirConstant constant) {
            this.constant = constant;
        }

        @NotNull
        public MirConstant getConstant() {
            return constant;
        }

        @NotNull
        @Override
        public MirOperand toCopy() {
            return this;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Constant that = (Constant) o;
            return Objects.equals(constant, that.constant);
        }

        @Override
        public int hashCode() {
            return Objects.hash(constant);
        }

        @Override
        public String toString() {
            return "Constant(constant=" + constant + ")";
        }
    }

    public static final class Move extends MirOperand {
        @NotNull
        private final MirPlace place;

        public Move(@NotNull MirPlace place) {
            this.place = place;
        }

        @NotNull
        public MirPlace getPlace() {
            return place;
        }

        @NotNull
        @Override
        public MirOperand toCopy() {
            return new Copy(place);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Move move = (Move) o;
            return Objects.equals(place, move.place);
        }

        @Override
        public int hashCode() {
            return Objects.hash(place);
        }

        @Override
        public String toString() {
            return "Move(place=" + place + ")";
        }
    }

    public static final class Copy extends MirOperand {
        @NotNull
        private final MirPlace place;

        public Copy(@NotNull MirPlace place) {
            this.place = place;
        }

        @NotNull
        public MirPlace getPlace() {
            return place;
        }

        @NotNull
        @Override
        public MirOperand toCopy() {
            return this;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Copy copy = (Copy) o;
            return Objects.equals(place, copy.place);
        }

        @Override
        public int hashCode() {
            return Objects.hash(place);
        }

        @Override
        public String toString() {
            return "Copy(place=" + place + ")";
        }
    }
}
