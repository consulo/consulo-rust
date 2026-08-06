/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.psi.ext;

import jakarta.annotation.Nonnull;
import org.rust.lang.core.psi.*;
import org.rust.lang.core.types.ty.Ty;

import java.util.Collections;
import java.util.List;

public final class RsTraitItemExtUtil {
    private RsTraitItemExtUtil() {}

    @Nonnull
    public static Ty getDeclaredType(@Nonnull RsTraitItem trait) {
        return org.rust.lang.core.types.RsPsiTypeImplUtil.declaredType(trait);
    }

    @Nonnull
    public static List<RsAbstractable> getExpandedMembers(@Nonnull RsTraitItem trait) {
        return RsMembersUtil.getExpandedMembers(trait);
    }
}
