/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.mir.dataflow.framework;

import jakarta.annotation.Nonnull;
import org.rust.lang.core.mir.schemas.MirLocation;

import java.util.Objects;

public abstract class TwoPhaseActivation {
    private TwoPhaseActivation() {
    }

    public static final TwoPhaseActivation NOT_TWO_PHASE = new TwoPhaseActivation() {
        @Override
        public String toString() {
            return "NotTwoPhase";
        }
    };

    public static final TwoPhaseActivation NOT_ACTIVATED = new TwoPhaseActivation() {
        @Override
        public String toString() {
            return "NotActivated";
        }
    };

    public static final class ActivatedAt extends TwoPhaseActivation {
        @Nonnull
        private final MirLocation location;

        public ActivatedAt(@Nonnull MirLocation location) {
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
            ActivatedAt that = (ActivatedAt) o;
            return Objects.equals(location, that.location);
        }

        @Override
        public int hashCode() {
            return Objects.hash(location);
        }

        @Override
        public String toString() {
            return "ActivatedAt(location=" + location + ")";
        }
    }
}
