/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.dfa.borrowck.gatherLoans;

import org.jetbrains.annotations.NotNull;
import org.rust.lang.core.psi.ext.RsStructOrEnumItemElement;
import org.rust.lang.core.types.ty.Ty;

/**
 * and {@code Ty.isAdtWithDestructor} which are now in {@link GatherMoves}.
 */
public final class HasDestructorUtil {
    private HasDestructorUtil() {
    }

    public static boolean getHasDestructor(@NotNull RsStructOrEnumItemElement element) {
        return GatherMoves.hasDestructor(element);
    }

    public static boolean isAdtWithDestructor(@NotNull Ty ty) {
        return GatherMoves.isAdtWithDestructor(ty);
    }
}
