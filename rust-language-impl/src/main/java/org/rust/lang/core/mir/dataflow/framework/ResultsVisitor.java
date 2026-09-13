/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.mir.dataflow.framework;

import jakarta.annotation.Nonnull;
import org.rust.lang.core.mir.schemas.*;

public interface ResultsVisitor<FlowState> {
    default void visitBlockStart(@Nonnull FlowState state, @Nonnull MirBasicBlock block) {
    }

    default void visitBlockEnd(@Nonnull FlowState state, @Nonnull MirBasicBlock block) {
    }

    default void visitStatementBeforePrimaryEffect(@Nonnull FlowState state, @Nonnull MirStatement statement, @Nonnull MirLocation location) {
    }

    default void visitStatementAfterPrimaryEffect(@Nonnull FlowState state, @Nonnull MirStatement statement, @Nonnull MirLocation location) {
    }

    default void visitTerminatorBeforePrimaryEffect(@Nonnull FlowState state, @Nonnull MirTerminator<MirBasicBlock> terminator, @Nonnull MirLocation location) {
    }

    default void visitTerminatorAfterPrimaryEffect(@Nonnull FlowState state, @Nonnull MirTerminator<MirBasicBlock> terminator, @Nonnull MirLocation location) {
    }
}
