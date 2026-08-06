/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.mir.schemas;

import jakarta.annotation.Nonnull;
import org.rust.lang.core.mir.WithIndex;

import java.util.List;

public interface MirBasicBlock extends WithIndex {
    @Override
    int getIndex();

    @Nonnull
    List<MirStatement> getStatements();

    @Nonnull
    MirTerminator<MirBasicBlock> getTerminator();

    boolean getUnwind();

    @Nonnull
    default MirLocation getTerminatorLocation() {
        return new MirLocation(this, getStatements().size());
    }
}
