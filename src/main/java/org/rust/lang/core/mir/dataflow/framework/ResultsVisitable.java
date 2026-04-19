/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.mir.dataflow.framework;

import org.jetbrains.annotations.NotNull;
import org.rust.lang.core.mir.schemas.*;

/**
 * Things that can be visited by a {@link ResultsVisitor}.
 * It exists so that we can visit the results of multiple dataflow analyses simultaneously.
 */
public interface ResultsVisitable<FlowState> {
    @NotNull
    Direction getDirection();

    @NotNull
    FlowState getCopyOfBlockState(@NotNull MirBasicBlock block);

    void reconstructBeforeStatementEffect(@NotNull FlowState state, @NotNull MirStatement statement, @NotNull MirLocation location);

    void reconstructStatementEffect(@NotNull FlowState state, @NotNull MirStatement statement, @NotNull MirLocation location);

    void reconstructBeforeTerminatorEffect(@NotNull FlowState state, @NotNull MirTerminator<MirBasicBlock> terminator, @NotNull MirLocation location);

    void reconstructTerminatorEffect(@NotNull FlowState state, @NotNull MirTerminator<MirBasicBlock> terminator, @NotNull MirLocation location);
}
