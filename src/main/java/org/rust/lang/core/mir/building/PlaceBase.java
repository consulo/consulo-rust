/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.mir.building;

import org.jetbrains.annotations.NotNull;
import org.rust.lang.core.mir.schemas.MirLocal;

import java.util.Objects;

public abstract class PlaceBase {
    private PlaceBase() {
    }

    public static final class Local extends PlaceBase {
        @NotNull
        private final MirLocal local;

        public Local(@NotNull MirLocal local) {
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
            Local that = (Local) o;
            return Objects.equals(local, that.local);
        }

        @Override
        public int hashCode() {
            return Objects.hash(local);
        }

        @Override
        public String toString() {
            return "Local(local=" + local + ")";
        }
    }
    // TODO: upvar
}
