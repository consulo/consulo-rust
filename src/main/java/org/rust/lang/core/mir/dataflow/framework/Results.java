/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.mir.dataflow.framework;

import org.jetbrains.annotations.NotNull;
import org.rust.lang.core.mir.schemas.*;

import java.util.List;

/** A dataflow analysis that has converged to fixpoint. */
public class Results<Domain> implements ResultsVisitable<Domain> {
    @NotNull
    private final Analysis<Domain> analysis;
    @NotNull
    private final List<Domain> blockStates;

    public Results(@NotNull Analysis<Domain> analysis, @NotNull List<Domain> blockStates) {
        this.analysis = analysis;
        this.blockStates = blockStates;
    }

    @NotNull
    public Analysis<Domain> getAnalysis() {
        return analysis;
    }

    @NotNull
    public List<Domain> getBlockStates() {
        return blockStates;
    }

    @Override
    @NotNull
    public Direction getDirection() {
        return analysis.getDirection();
    }

    @Override
    @NotNull
    public Domain getCopyOfBlockState(@NotNull MirBasicBlock block) {
        return analysis.copyState(blockStates.get(block.getIndex()));
    }

    @Override
    public void reconstructBeforeStatementEffect(@NotNull Domain state, @NotNull MirStatement statement, @NotNull MirLocation location) {
        analysis.applyBeforeStatementEffect(state, statement, location);
    }

    @Override
    public void reconstructStatementEffect(@NotNull Domain state, @NotNull MirStatement statement, @NotNull MirLocation location) {
        analysis.applyStatementEffect(state, statement, location);
    }

    @Override
    public void reconstructBeforeTerminatorEffect(@NotNull Domain state, @NotNull MirTerminator<MirBasicBlock> terminator, @NotNull MirLocation location) {
        analysis.applyBeforeTerminatorEffect(state, terminator, location);
    }

    @Override
    public void reconstructTerminatorEffect(@NotNull Domain state, @NotNull MirTerminator<MirBasicBlock> terminator, @NotNull MirLocation location) {
        analysis.applyTerminatorEffect(state, terminator, location);
    }
}
