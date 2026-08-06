/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.mir.dataflow.move;

import jakarta.annotation.Nonnull;
import org.rust.lang.core.mir.schemas.MirLocal;
import org.rust.lang.core.mir.schemas.MirLocation;

import java.util.Objects;

/**
 * Initializations can be from an argument or from a statement. Arguments
 * do not have locations, in those cases the Local is kept.
 */
public interface InitLocation {

    final class Argument implements InitLocation {
        @Nonnull
        private final MirLocal local;

        public Argument(@Nonnull MirLocal local) {
            this.local = local;
        }

        @Nonnull
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
        @Nonnull
        private final MirLocation location;

        public Statement(@Nonnull MirLocation location) {
            this.location = location;
        }

        @Nonnull
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
