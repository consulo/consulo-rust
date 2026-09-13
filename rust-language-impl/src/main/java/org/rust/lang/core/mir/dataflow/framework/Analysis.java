/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.mir.dataflow.framework;

import jakarta.annotation.Nonnull;
import org.rust.lang.core.mir.schemas.*;

public interface Analysis<Domain> {
    @Nonnull
    Direction getDirection();

    @Nonnull
    Domain bottomValue(@Nonnull MirBody body);

    void initializeStartBlock(@Nonnull MirBody body, @Nonnull Domain state);

    boolean join(@Nonnull Domain state1, @Nonnull Domain state2);

    @Nonnull
    Domain copyState(@Nonnull Domain state);

    default void applyBeforeStatementEffect(@Nonnull Domain state, @Nonnull MirStatement statement, @Nonnull MirLocation location) {
    }

    void applyStatementEffect(@Nonnull Domain state, @Nonnull MirStatement statement, @Nonnull MirLocation location);

    default void applyBeforeTerminatorEffect(@Nonnull Domain state, @Nonnull MirTerminator<MirBasicBlock> terminator, @Nonnull MirLocation location) {
    }

    void applyTerminatorEffect(@Nonnull Domain state, @Nonnull MirTerminator<MirBasicBlock> terminator, @Nonnull MirLocation location);

    /**
     * Updates the current dataflow state with the effect of a successful return from a {@link MirTerminator.Call}.
     * This is separate from {@link #applyTerminatorEffect} to properly track state across unwind edges.
     */
    default void applyCallReturnEffect(@Nonnull Domain state, @Nonnull MirBasicBlock block, @Nonnull MirPlace returnPlace) {
    }

    @Nonnull
    default Engine<Domain> intoEngine(@Nonnull MirBody body) {
        return new Engine<>(body, this);
    }
}
