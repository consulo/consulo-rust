/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.types.infer;

import org.jetbrains.annotations.NotNull;
import org.rust.lang.core.types.consts.Const;
import org.rust.lang.core.types.regions.Region;
import org.rust.lang.core.types.ty.Ty;

public interface TypeFolder {
    @NotNull
    default Ty foldTy(@NotNull Ty ty) {
        return ty;
    }

    @NotNull
    default Region foldRegion(@NotNull Region region) {
        return region;
    }

    @NotNull
    default Const foldConst(@NotNull Const aConst) {
        return aConst;
    }
}
