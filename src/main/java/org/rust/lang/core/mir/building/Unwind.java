/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.mir.building;

import org.jetbrains.annotations.NotNull;
import org.rust.lang.core.mir.schemas.impls.MirBasicBlockImpl;

public class Unwind implements DropTreeBuilder {
    @NotNull
    private final BasicBlocksBuilder basicBlocks;

    public Unwind(@NotNull BasicBlocksBuilder basicBlocks) {
        this.basicBlocks = basicBlocks;
    }

    @Override
    @NotNull
    public MirBasicBlockImpl makeBlock() {
        return basicBlocks.newBlock(true);
    }

    @Override
    public void addEntry(@NotNull MirBasicBlockImpl from, @NotNull MirBasicBlockImpl to) {
        from.unwindTerminatorTo(to);
    }
}
