/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.mir.dataflow.framework;

import org.jetbrains.annotations.NotNull;
import org.rust.lang.core.mir.schemas.*;

public interface Analysis<Domain> {
    @NotNull
    Direction getDirection();

    @NotNull
    Domain bottomValue(@NotNull MirBody body);

    void initializeStartBlock(@NotNull MirBody body, @NotNull Domain state);

    boolean join(@NotNull Domain state1, @NotNull Domain state2);

    @NotNull
    Domain copyState(@NotNull Domain state);

    default void applyBeforeStatementEffect(@NotNull Domain state, @NotNull MirStatement statement, @NotNull MirLocation location) {
    }

    void applyStatementEffect(@NotNull Domain state, @NotNull MirStatement statement, @NotNull MirLocation location);

    default void applyBeforeTerminatorEffect(@NotNull Domain state, @NotNull MirTerminator<MirBasicBlock> terminator, @NotNull MirLocation location) {
    }

    void applyTerminatorEffect(@NotNull Domain state, @NotNull MirTerminator<MirBasicBlock> terminator, @NotNull MirLocation location);

    /**
     * Updates the current dataflow state with the effect of a successful return from a {@link MirTerminator.Call}.
     * This is separate from {@link #applyTerminatorEffect} to properly track state across unwind edges.
     */
    default void applyCallReturnEffect(@NotNull Domain state, @NotNull MirBasicBlock block, @NotNull MirPlace returnPlace) {
    }

    @NotNull
    default Engine<Domain> intoEngine(@NotNull MirBody body) {
        return new Engine<>(body, this);
    }
}
