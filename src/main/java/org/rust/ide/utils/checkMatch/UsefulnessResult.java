/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.utils.checkMatch;

import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.List;

public abstract class UsefulnessResult {
    private UsefulnessResult() {
    }

    public boolean isUseful() {
        return this != USELESS;
    }

    public static final class UsefulWithWitness extends UsefulnessResult {
        @NotNull
        private final List<Witness> witnesses;

        public UsefulWithWitness(@NotNull List<Witness> witnesses) {
            this.witnesses = witnesses;
        }

        @NotNull
        public List<Witness> getWitnesses() {
            return witnesses;
        }

        @NotNull
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
