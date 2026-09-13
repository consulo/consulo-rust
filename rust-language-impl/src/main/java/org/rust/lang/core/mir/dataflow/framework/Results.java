/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.mir.dataflow.framework;

import jakarta.annotation.Nonnull;
import org.rust.lang.core.mir.schemas.*;

import java.util.List;

/** A dataflow analysis that has converged to fixpoint. */
public class Results<Domain> implements ResultsVisitable<Domain> {
    @Nonnull
    private final Analysis<Domain> analysis;
    @Nonnull
    private final List<Domain> blockStates;

    public Results(@Nonnull Analysis<Domain> analysis, @Nonnull List<Domain> blockStates) {
        this.analysis = analysis;
        this.blockStates = blockStates;
    }

    @Nonnull
    public Analysis<Domain> getAnalysis() {
        return analysis;
    }

    @Nonnull
    public List<Domain> getBlockStates() {
        return blockStates;
    }

    @Override
    @Nonnull
    public Direction getDirection() {
        return analysis.getDirection();
    }

    @Override
    @Nonnull
    public Domain getCopyOfBlockState(@Nonnull MirBasicBlock block) {
        return analysis.copyState(blockStates.get(block.getIndex()));
    }

    @Override
    public void reconstructBeforeStatementEffect(@Nonnull Domain state, @Nonnull MirStatement statement, @Nonnull MirLocation location) {
        analysis.applyBeforeStatementEffect(state, statement, location);
    }

    @Override
    public void reconstructStatementEffect(@Nonnull Domain state, @Nonnull MirStatement statement, @Nonnull MirLocation location) {
        analysis.applyStatementEffect(state, statement, location);
    }

    @Override
    public void reconstructBeforeTerminatorEffect(@Nonnull Domain state, @Nonnull MirTerminator<MirBasicBlock> terminator, @Nonnull MirLocation location) {
        analysis.applyBeforeTerminatorEffect(state, terminator, location);
    }

    @Override
    public void reconstructTerminatorEffect(@Nonnull Domain state, @Nonnull MirTerminator<MirBasicBlock> terminator, @Nonnull MirLocation location) {
        analysis.applyTerminatorEffect(state, terminator, location);
    }
}
