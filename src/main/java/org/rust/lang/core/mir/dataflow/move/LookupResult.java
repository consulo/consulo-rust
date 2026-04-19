/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.mir.dataflow.move;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

public interface LookupResult {

    final class Exact implements LookupResult {
        @NotNull
        private final MovePath movePath;

        public Exact(@NotNull MovePath movePath) {
            this.movePath = movePath;
        }

        @NotNull
        public MovePath getMovePath() {
            return movePath;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Exact exact = (Exact) o;
            return Objects.equals(movePath, exact.movePath);
        }

        @Override
        public int hashCode() {
            return Objects.hash(movePath);
        }

        @Override
        public String toString() {
            return "Exact(movePath=" + movePath + ")";
        }
    }

    final class Parent implements LookupResult {
        @Nullable
        private final MovePath movePath;

        public Parent(@Nullable MovePath movePath) {
            this.movePath = movePath;
        }

        @Nullable
        public MovePath getMovePath() {
            return movePath;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Parent parent = (Parent) o;
            return Objects.equals(movePath, parent.movePath);
        }

        @Override
        public int hashCode() {
            return Objects.hash(movePath);
        }

        @Override
        public String toString() {
            return "Parent(movePath=" + movePath + ")";
        }
    }
}
