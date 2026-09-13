/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.mir.dataflow.framework;

import jakarta.annotation.Nonnull;
import org.rust.lang.core.mir.schemas.MirBasicBlock;

import java.util.function.BiConsumer;

public interface Direction {
    <Domain> void applyEffectsInBlock(
        @Nonnull Analysis<Domain> analysis,
        @Nonnull Domain state,
        @Nonnull MirBasicBlock block
    );

    <Domain> void joinStateIntoSuccessorsOf(
        @Nonnull Analysis<Domain> analysis,
        @Nonnull Domain exitState,
        @Nonnull MirBasicBlock block,
        @Nonnull BiConsumer<MirBasicBlock, Domain> propagate
    );

    <FlowState> void visitResultsInBlock(
        @Nonnull MirBasicBlock block,
        @Nonnull ResultsVisitable<FlowState> results,
        @Nonnull ResultsVisitor<FlowState> visitor
    );
}
