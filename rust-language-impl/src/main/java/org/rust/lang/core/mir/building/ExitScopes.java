/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.mir.building;

import jakarta.annotation.Nonnull;
import org.rust.lang.core.mir.schemas.impls.MirBasicBlockImpl;

public class ExitScopes implements DropTreeBuilder {
    @Nonnull
    private final BasicBlocksBuilder basicBlocks;

    public ExitScopes(@Nonnull BasicBlocksBuilder basicBlocks) {
        this.basicBlocks = basicBlocks;
    }

    @Override
    @Nonnull
    public MirBasicBlockImpl makeBlock() {
        return basicBlocks.newBlock();
    }

    @Override
    public void addEntry(@Nonnull MirBasicBlockImpl from, @Nonnull MirBasicBlockImpl to) {
        from.terminateWithGoto(to, null);
    }
}
