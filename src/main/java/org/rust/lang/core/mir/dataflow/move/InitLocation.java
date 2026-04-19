/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.mir.dataflow.move;

import org.jetbrains.annotations.NotNull;
import org.rust.lang.core.mir.schemas.MirLocal;
import org.rust.lang.core.mir.schemas.MirLocation;

import java.util.Objects;

/**
 * Initializations can be from an argument or from a statement. Arguments
 * do not have locations, in those cases the Local is kept.
 */
public interface InitLocation {

    final class Argument implements InitLocation {
        @NotNull
        private final MirLocal local;

        public Argument(@NotNull MirLocal local) {
            this.local = local;
        }

        @NotNull
        public MirLocal getLocal() {
            return local;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Argument that = (Argument) o;
            return Objects.equals(local, that.local);
        }

        @Override
        public int hashCode() {
            return Objects.hash(local);
        }

        @Override
        public String toString() {
            return "Argument(local=" + local + ")";
        }
    }

    final class Statement implements InitLocation {
        @NotNull
        private final MirLocation location;

        public Statement(@NotNull MirLocation location) {
            this.location = location;
        }

        @NotNull
        public MirLocation getLocation() {
            return location;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Statement that = (Statement) o;
            return Objects.equals(location, that.location);
        }

        @Override
        public int hashCode() {
            return Objects.hash(location);
        }

        @Override
        public String toString() {
            return "Statement(location=" + location + ")";
        }
    }
}
