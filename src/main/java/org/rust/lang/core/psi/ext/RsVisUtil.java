/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.psi.ext;

import org.jetbrains.annotations.NotNull;
import org.rust.lang.core.psi.RsVis;

public final class RsVisUtil {
    private RsVisUtil() {
    }

    @NotNull
    public static RsVisibility getVisibility(@NotNull RsVis vis) {
        return RsVisibilityUtil.getVisibility(vis);
    }
}
