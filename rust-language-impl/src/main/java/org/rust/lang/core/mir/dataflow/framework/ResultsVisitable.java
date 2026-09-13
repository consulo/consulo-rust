/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.mir.dataflow.framework;

import jakarta.annotation.Nonnull;
import org.rust.lang.core.mir.schemas.*;

/**
 * Things that can be visited by a {@link ResultsVisitor}.
 * It exists so that we can visit the results of multiple dataflow analyses simultaneously.
 */
public interface ResultsVisitable<FlowState> {
    @Nonnull
    Direction getDirection();

    @Nonnull
    FlowState getCopyOfBlockState(@Nonnull MirBasicBlock block);

    void reconstructBeforeStatementEffect(@Nonnull FlowState state, @Nonnull MirStatement statement, @Nonnull MirLocation location);

    void reconstructStatementEffect(@Nonnull FlowState state, @Nonnull MirStatement statement, @Nonnull MirLocation location);

    void reconstructBeforeTerminatorEffect(@Nonnull FlowState state, @Nonnull MirTerminator<MirBasicBlock> terminator, @Nonnull MirLocation location);

    void reconstructTerminatorEffect(@Nonnull FlowState state, @Nonnull MirTerminator<MirBasicBlock> terminator, @Nonnull MirLocation location);
}
