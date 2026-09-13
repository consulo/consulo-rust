/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.mir.building;

import jakarta.annotation.Nonnull;
import org.rust.lang.core.mir.schemas.impls.MirBasicBlockImpl;

import java.util.ArrayList;
import java.util.List;

public class BasicBlocksBuilder {
    private final List<MirBasicBlockImpl> basicBlocks = new ArrayList<>();

    public BasicBlocksBuilder() {
        newBlock(false);
    }

    @Nonnull
    public BlockAnd<Void> startBlock() {
        return BlockAnd.of(basicBlocks.get(0), null);
    }

    @Nonnull
    public MirBasicBlockImpl newBlock() {
        return newBlock(false);
    }

    @Nonnull
    public MirBasicBlockImpl newBlock(boolean unwind) {
        MirBasicBlockImpl bb = new MirBasicBlockImpl(basicBlocks.size(), unwind);
        basicBlocks.add(bb);
        return bb;
    }

    @Nonnull
    public List<MirBasicBlockImpl> build() {
        return basicBlocks;
    }
}
