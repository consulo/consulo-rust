/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.mir.building;

import org.jetbrains.annotations.NotNull;
import org.rust.lang.core.mir.schemas.impls.MirBasicBlockImpl;

public interface DropTreeBuilder {
    @NotNull
    MirBasicBlockImpl makeBlock();

    void addEntry(@NotNull MirBasicBlockImpl from, @NotNull MirBasicBlockImpl to);
}
