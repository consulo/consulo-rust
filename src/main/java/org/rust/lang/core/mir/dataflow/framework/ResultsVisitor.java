/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.mir.dataflow.framework;

import org.jetbrains.annotations.NotNull;
import org.rust.lang.core.mir.schemas.*;

public interface ResultsVisitor<FlowState> {
    default void visitBlockStart(@NotNull FlowState state, @NotNull MirBasicBlock block) {
    }

    default void visitBlockEnd(@NotNull FlowState state, @NotNull MirBasicBlock block) {
    }

    default void visitStatementBeforePrimaryEffect(@NotNull FlowState state, @NotNull MirStatement statement, @NotNull MirLocation location) {
    }

    default void visitStatementAfterPrimaryEffect(@NotNull FlowState state, @NotNull MirStatement statement, @NotNull MirLocation location) {
    }

    default void visitTerminatorBeforePrimaryEffect(@NotNull FlowState state, @NotNull MirTerminator<MirBasicBlock> terminator, @NotNull MirLocation location) {
    }

    default void visitTerminatorAfterPrimaryEffect(@NotNull FlowState state, @NotNull MirTerminator<MirBasicBlock> terminator, @NotNull MirLocation location) {
    }
}
