/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.psi.ext.impl;

import jakarta.annotation.Nonnull;
import org.rust.lang.core.psi.RsVis;
import org.rust.lang.core.psi.ext.*;

public final class RsVisUtil {
    private RsVisUtil() {
    }

    @Nonnull
    public static RsVisibility getVisibility(@Nonnull RsVis vis) {
        return RsVisibilityUtil.getVisibility(vis);
    }
}
