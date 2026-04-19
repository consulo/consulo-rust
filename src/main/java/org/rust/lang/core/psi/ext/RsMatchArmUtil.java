/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.psi.ext;

import org.jetbrains.annotations.NotNull;
import org.rust.lang.core.psi.RsMatchArm;
import org.rust.lang.core.psi.RsOrPat;
import org.rust.lang.core.psi.RsPat;

import java.util.Collections;
import java.util.List;

public final class RsMatchArmUtil {
    private RsMatchArmUtil() {
    }

    /**
     * @deprecated Support {@code RsOrPat}
     */
    @Deprecated
    @NotNull
    public static List<RsPat> getPatList(@NotNull RsMatchArm arm) {
        RsPat pat = arm.getPat();
        if (pat instanceof RsOrPat) {
            return ((RsOrPat) pat).getPatList();
        }
        return Collections.singletonList(pat);
    }
}
