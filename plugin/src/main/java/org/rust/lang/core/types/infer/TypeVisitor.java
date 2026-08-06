/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.types.infer;

import jakarta.annotation.Nonnull;
import org.rust.lang.core.types.consts.Const;
import org.rust.lang.core.types.regions.Region;
import org.rust.lang.core.types.ty.Ty;

public interface TypeVisitor {
    default boolean visitTy(@Nonnull Ty ty) {
        return false;
    }

    default boolean visitRegion(@Nonnull Region region) {
        return false;
    }

    default boolean visitConst(@Nonnull Const aConst) {
        return false;
    }
}
