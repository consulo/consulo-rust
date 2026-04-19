/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.mir.building;

import org.jetbrains.annotations.NotNull;
import org.rust.lang.core.mir.schemas.impls.MirBasicBlockImpl;

public class ExitScopes implements DropTreeBuilder {
    @NotNull
    private final BasicBlocksBuilder basicBlocks;

    public ExitScopes(@NotNull BasicBlocksBuilder basicBlocks) {
        this.basicBlocks = basicBlocks;
    }

    @Override
    @NotNull
    public MirBasicBlockImpl makeBlock() {
        return basicBlocks.newBlock();
    }

    @Override
    public void addEntry(@NotNull MirBasicBlockImpl from, @NotNull MirBasicBlockImpl to) {
        from.terminateWithGoto(to, null);
    }
}
