/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.mir.schemas;

import org.jetbrains.annotations.NotNull;
import org.rust.lang.core.mir.WithIndex;

import java.util.List;

public interface MirBasicBlock extends WithIndex {
    @Override
    int getIndex();

    @NotNull
    List<MirStatement> getStatements();

    @NotNull
    MirTerminator<MirBasicBlock> getTerminator();

    boolean getUnwind();

    @NotNull
    default MirLocation getTerminatorLocation() {
        return new MirLocation(this, getStatements().size());
    }
}
