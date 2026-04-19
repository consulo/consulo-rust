/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.resolve2;

import org.jetbrains.annotations.NotNull;

import java.util.Objects;

public abstract class PartialResolvedImport {

    private PartialResolvedImport() {}

    /** None of any namespaces is resolved */
    public static final class Unresolved extends PartialResolvedImport {
        public static final Unresolved INSTANCE = new Unresolved();
        private Unresolved() {}
    }

    /** One of namespaces is resolved */
    public static final class Indeterminate extends PartialResolvedImport {
        @NotNull
        private final PerNs perNs;

        public Indeterminate(@NotNull PerNs perNs) {
            this.perNs = perNs;
        }

        @NotNull
        public PerNs getPerNs() {
            return perNs;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof Indeterminate)) return false;
            return Objects.equals(perNs, ((Indeterminate) o).perNs);
        }

        @Override
        public int hashCode() {
            return Objects.hash(perNs);
        }
    }

    /** All namespaces are resolved, OR it came from other crate */
    public static final class Resolved extends PartialResolvedImport {
        @NotNull
        private final PerNs perNs;

        public Resolved(@NotNull PerNs perNs) {
            this.perNs = perNs;
        }

        @NotNull
        public PerNs getPerNs() {
            return perNs;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof Resolved)) return false;
            return Objects.equals(perNs, ((Resolved) o).perNs);
        }

        @Override
        public int hashCode() {
            return Objects.hash(perNs);
        }
    }
}
