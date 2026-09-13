/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.types.infer;

import jakarta.annotation.Nonnull;
import org.rust.lang.core.types.consts.Const;
import org.rust.lang.core.types.regions.Region;
import org.rust.lang.core.types.ty.Ty;

public interface TypeFolder {
    @Nonnull
    default Ty foldTy(@Nonnull Ty ty) {
        return ty;
    }

    @Nonnull
    default Region foldRegion(@Nonnull Region region) {
        return region;
    }

    @Nonnull
    default Const foldConst(@Nonnull Const aConst) {
        return aConst;
    }
}
