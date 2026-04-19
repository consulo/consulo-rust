/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.psi.ext;

import org.jetbrains.annotations.NotNull;
import org.rust.lang.core.psi.*;
import org.rust.lang.core.types.ty.Ty;

import java.util.Collections;
import java.util.List;

public final class RsTraitItemExtUtil {
    private RsTraitItemExtUtil() {}

    @NotNull
    public static Ty getDeclaredType(@NotNull RsTraitItem trait) {
        return org.rust.lang.core.types.RsPsiTypeImplUtil.declaredType(trait);
    }

    @NotNull
    public static List<RsAbstractable> getExpandedMembers(@NotNull RsTraitItem trait) {
        return RsMembersUtil.getExpandedMembers(trait);
    }
}
