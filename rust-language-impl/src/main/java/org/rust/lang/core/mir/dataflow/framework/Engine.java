/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.mir.dataflow.framework;

import jakarta.annotation.Nonnull;
import org.rust.lang.core.mir.schemas.MirBasicBlock;
import org.rust.lang.core.mir.schemas.MirBody;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** A solver for dataflow problems. */
public class Engine<Domain> {
    @Nonnull
    private final MirBody body;
    @Nonnull
    private final Analysis<Domain> analysis;
    @Nonnull
    private final List<Domain> blockStates;

    public Engine(@Nonnull MirBody body, @Nonnull Analysis<Domain> analysis) {
        this.body = body;
        this.analysis = analysis;

        List<Domain> states = new ArrayList<>(body.getBasicBlocks().size());
        for (int i = 0; i < body.getBasicBlocks().size(); i++) {
            states.add(analysis.bottomValue(body));
        }
        this.blockStates = states;

        analysis.initializeStartBlock(body, blockStates.get(0));
    }

    @Nonnull
    public Results<Domain> iterateToFixPoint() {
        WorkQueue<MirBasicBlock> dirtyQueue = new WorkQueue<>(body.getBasicBlocks().size());
        for (MirBasicBlock block : orderBasicBlocks()) {
            dirtyQueue.insert(block);
        }

        while (!dirtyQueue.isEmpty()) {
            MirBasicBlock block = dirtyQueue.pop();
            Domain initialBlockState = blockStates.get(block.getIndex());
            Domain blockState = analysis.copyState(initialBlockState);
            analysis.getDirection().applyEffectsInBlock(analysis, blockState, block);
            analysis.getDirection().joinStateIntoSuccessorsOf(analysis, blockState, block, (target, targetState) -> {
                boolean changed = analysis.join(blockStates.get(target.getIndex()), targetState);
                if (changed) {
                    dirtyQueue.insert(target);
                }
            });
        }
        return new Results<>(analysis, blockStates);
    }

    /** Reverse post order for {@link Forward} direction */
    @Nonnull
    private List<MirBasicBlock> orderBasicBlocks() {
        List<MirBasicBlock> result = Utils.getBasicBlocksInPostOrder(body);
        if (analysis.getDirection() instanceof Forward) {
            List<MirBasicBlock> reversed = new ArrayList<>(result);
            Collections.reverse(reversed);
            return reversed;
        }
        return result;
    }
}
