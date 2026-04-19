/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.mir.dataflow.framework;

import org.jetbrains.annotations.NotNull;
import org.rust.lang.core.mir.schemas.MirBasicBlock;

import java.util.function.BiConsumer;

public interface Direction {
    <Domain> void applyEffectsInBlock(
        @NotNull Analysis<Domain> analysis,
        @NotNull Domain state,
        @NotNull MirBasicBlock block
    );

    <Domain> void joinStateIntoSuccessorsOf(
        @NotNull Analysis<Domain> analysis,
        @NotNull Domain exitState,
        @NotNull MirBasicBlock block,
        @NotNull BiConsumer<MirBasicBlock, Domain> propagate
    );

    <FlowState> void visitResultsInBlock(
        @NotNull MirBasicBlock block,
        @NotNull ResultsVisitable<FlowState> results,
        @NotNull ResultsVisitor<FlowState> visitor
    );
}
