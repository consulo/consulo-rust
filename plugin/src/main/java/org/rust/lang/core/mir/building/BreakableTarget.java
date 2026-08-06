/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.mir.building;

import jakarta.annotation.Nonnull;
import org.rust.lang.core.types.regions.Scope;

import java.util.Objects;

public abstract class BreakableTarget {
    private BreakableTarget() {
    }

    public static final class Break extends BreakableTarget {
        @Nonnull
        private final Scope scope;

        public Break(@Nonnull Scope scope) {
            this.scope = scope;
        }

        @Nonnull
        public Scope getScope() {
            return scope;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Break that = (Break) o;
            return Objects.equals(scope, that.scope);
        }

        @Override
        public int hashCode() {
            return Objects.hash(scope);
        }

        @Override
        public String toString() {
            return "Break(scope=" + scope + ")";
        }
    }
}
