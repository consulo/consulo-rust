/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.mir.building;

import jakarta.annotation.Nonnull;
import org.rust.lang.core.mir.schemas.impls.MirBasicBlockImpl;

public interface DropTreeBuilder {
    @Nonnull
    MirBasicBlockImpl makeBlock();

    void addEntry(@Nonnull MirBasicBlockImpl from, @Nonnull MirBasicBlockImpl to);
}
