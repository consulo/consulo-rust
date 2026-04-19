/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.psi.ext;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.rust.lang.core.psi.RsLetExpr;
import org.rust.lang.core.psi.RsOrPat;
import org.rust.lang.core.psi.RsPat;

import java.util.Collections;
import java.util.List;

public final class RsLetExprUtil {
    private RsLetExprUtil() {
    }

    /**
     * @deprecated Support RsOrPat
     */
    @Deprecated
    @Nullable
    public static List<RsPat> getPatList(@NotNull RsLetExpr letExpr) {
        RsPat pat = letExpr.getPat();
        if (pat == null) return null;
        if (pat instanceof RsOrPat) {
            return ((RsOrPat) pat).getPatList();
        }
        return Collections.singletonList(pat);
    }
}
