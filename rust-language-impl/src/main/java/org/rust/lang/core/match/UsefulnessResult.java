/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.match;

import jakarta.annotation.Nonnull;

import java.util.Collections;
import java.util.List;

public abstract class UsefulnessResult {
    private UsefulnessResult() {
    }

    public boolean isUseful() {
        return this != USELESS;
    }

    public static final class UsefulWithWitness extends UsefulnessResult {
        @Nonnull
        private final List<Witness> witnesses;

        public UsefulWithWitness(@Nonnull List<Witness> witnesses) {
            this.witnesses = witnesses;
        }

        @Nonnull
        public List<Witness> getWitnesses() {
            return witnesses;
        }

        @Nonnull
        public static UsefulWithWitness empty() {
            return new UsefulWithWitness(Collections.singletonList(new Witness()));
        }
    }

    public static final UsefulnessResult USEFUL = new UsefulnessResult() {
        @Override
        public String toString() {
            return "Useful";
        }
    };

    public static final UsefulnessResult USELESS = new UsefulnessResult() {
        @Override
        public boolean isUseful() {
            return false;
        }

        @Override
        public String toString() {
            return "Useless";
        }
    };
}
