/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.mir.dataflow.framework;

import jakarta.annotation.Nonnull;

import java.util.BitSet;

public interface GenKillAnalysis extends Analysis<BitSet> {

    @Override
    @Nonnull
    default BitSet copyState(@Nonnull BitSet state) {
        return (BitSet) state.clone();
    }

    @Override
    default boolean join(@Nonnull BitSet state1, @Nonnull BitSet state2) {
        // TODO optimize, do not copy
        BitSet old = (BitSet) state1.clone();
        state1.or(state2);
        return !state1.equals(old);
    }
}
