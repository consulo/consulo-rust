/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.types.infer;

import org.jetbrains.annotations.NotNull;
import org.rust.lang.core.types.consts.Const;
import org.rust.lang.core.types.regions.Region;
import org.rust.lang.core.types.ty.Ty;

public interface TypeVisitor {
    default boolean visitTy(@NotNull Ty ty) {
        return false;
    }

    default boolean visitRegion(@NotNull Region region) {
        return false;
    }

    default boolean visitConst(@NotNull Const aConst) {
        return false;
    }
}
