/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.mir.dataflow.impls;

import jakarta.annotation.Nonnull;
import org.rust.lang.core.mir.dataflow.framework.Direction;
import org.rust.lang.core.mir.dataflow.framework.Forward;
import org.rust.lang.core.mir.dataflow.framework.GenKillAnalysis;
import org.rust.lang.core.mir.dataflow.move.*;
import org.rust.lang.core.mir.schemas.*;
import org.rust.openapiext.TestAssertUtil;

import java.util.BitSet;

public class MaybeUninitializedPlaces implements GenKillAnalysis {
    @Nonnull
    private final MoveData moveData;

    public MaybeUninitializedPlaces(@Nonnull MoveData moveData) {
        this.moveData = moveData;
    }

    @Override
    @Nonnull
    public Direction getDirection() {
        return Forward.INSTANCE;
    }

    // bottom = all initialized
    @Override
    @Nonnull
    public BitSet bottomValue(@Nonnull MirBody body) {
        return new BitSet(moveData.getMovePathsCount());
    }

    // set all bits to 1 (uninit) before gathering counter-evidence
    @Override
    public void initializeStartBlock(@Nonnull MirBody body, @Nonnull BitSet state) {
        state.set(0, moveData.getMovePathsCount());
        DropFlagEffectUtil.dropFlagEffectsForFunctionEntry(body, moveData, (path, dropFlagState) -> {
            TestAssertUtil.testAssert(() -> dropFlagState == DropFlagState.Present);
            state.clear(path.getIndex());
        });
    }

    @Override
    public void applyStatementEffect(@Nonnull BitSet state, @Nonnull MirStatement statement, @Nonnull MirLocation location) {
        DropFlagEffectUtil.dropFlagEffectsForLocation(moveData, location, (movePath, movePathState) -> {
            updateBits(state, movePath, movePathState);
        });
    }

    @Override
    public void applyTerminatorEffect(@Nonnull BitSet state, @Nonnull MirTerminator<MirBasicBlock> terminator, @Nonnull MirLocation location) {
        DropFlagEffectUtil.dropFlagEffectsForLocation(moveData, location, (movePath, movePathState) -> {
            updateBits(state, movePath, movePathState);
        });
    }

    @Override
    public void applyCallReturnEffect(@Nonnull BitSet state, @Nonnull MirBasicBlock block, @Nonnull MirPlace returnPlace) {
        LookupResult lookupResult = moveData.getRevLookup().find(returnPlace);
        if (lookupResult instanceof LookupResult.Exact) {
            DropFlagEffectUtil.onAllChildrenBits(((LookupResult.Exact) lookupResult).getMovePath(), movePath -> {
                updateBits(state, movePath, DropFlagState.Present);
            });
        }
    }

    private void updateBits(@Nonnull BitSet state, @Nonnull MovePath movePath, @Nonnull DropFlagState movePathState) {
        state.set(movePath.getIndex(), movePathState == DropFlagState.Absent);
    }
}
