/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.mir.dataflow.impls;

import org.jetbrains.annotations.NotNull;
import org.rust.lang.core.mir.dataflow.framework.Direction;
import org.rust.lang.core.mir.dataflow.framework.Forward;
import org.rust.lang.core.mir.dataflow.framework.GenKillAnalysis;
import org.rust.lang.core.mir.dataflow.move.*;
import org.rust.lang.core.mir.schemas.*;
import org.rust.openapiext.TestAssertUtil;

import java.util.BitSet;

public class MaybeUninitializedPlaces implements GenKillAnalysis {
    @NotNull
    private final MoveData moveData;

    public MaybeUninitializedPlaces(@NotNull MoveData moveData) {
        this.moveData = moveData;
    }

    @Override
    @NotNull
    public Direction getDirection() {
        return Forward.INSTANCE;
    }

    // bottom = all initialized
    @Override
    @NotNull
    public BitSet bottomValue(@NotNull MirBody body) {
        return new BitSet(moveData.getMovePathsCount());
    }

    // set all bits to 1 (uninit) before gathering counter-evidence
    @Override
    public void initializeStartBlock(@NotNull MirBody body, @NotNull BitSet state) {
        state.set(0, moveData.getMovePathsCount());
        DropFlagEffectUtil.dropFlagEffectsForFunctionEntry(body, moveData, (path, dropFlagState) -> {
            TestAssertUtil.testAssert(() -> dropFlagState == DropFlagState.Present);
            state.clear(path.getIndex());
        });
    }

    @Override
    public void applyStatementEffect(@NotNull BitSet state, @NotNull MirStatement statement, @NotNull MirLocation location) {
        DropFlagEffectUtil.dropFlagEffectsForLocation(moveData, location, (movePath, movePathState) -> {
            updateBits(state, movePath, movePathState);
        });
    }

    @Override
    public void applyTerminatorEffect(@NotNull BitSet state, @NotNull MirTerminator<MirBasicBlock> terminator, @NotNull MirLocation location) {
        DropFlagEffectUtil.dropFlagEffectsForLocation(moveData, location, (movePath, movePathState) -> {
            updateBits(state, movePath, movePathState);
        });
    }

    @Override
    public void applyCallReturnEffect(@NotNull BitSet state, @NotNull MirBasicBlock block, @NotNull MirPlace returnPlace) {
        LookupResult lookupResult = moveData.getRevLookup().find(returnPlace);
        if (lookupResult instanceof LookupResult.Exact) {
            DropFlagEffectUtil.onAllChildrenBits(((LookupResult.Exact) lookupResult).getMovePath(), movePath -> {
                updateBits(state, movePath, DropFlagState.Present);
            });
        }
    }

    private void updateBits(@NotNull BitSet state, @NotNull MovePath movePath, @NotNull DropFlagState movePathState) {
        state.set(movePath.getIndex(), movePathState == DropFlagState.Absent);
    }
}
