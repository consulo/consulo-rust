/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.psi.ext.impl;

import jakarta.annotation.Nonnull;
import org.rust.lang.core.psi.RsMatchArm;
import org.rust.lang.core.psi.RsOrPat;
import org.rust.lang.core.psi.RsPat;

import java.util.Collections;
import java.util.List;
import org.rust.lang.core.psi.ext.*;

public final class RsMatchArmUtil {
    private RsMatchArmUtil() {
    }

    /**
     * @deprecated Support {@code RsOrPat}
     */
    @Deprecated
    @Nonnull
    public static List<RsPat> getPatList(@Nonnull RsMatchArm arm) {
        RsPat pat = arm.getPat();
        if (pat instanceof RsOrPat) {
            return ((RsOrPat) pat).getPatList();
        }
        return Collections.singletonList(pat);
    }
}
