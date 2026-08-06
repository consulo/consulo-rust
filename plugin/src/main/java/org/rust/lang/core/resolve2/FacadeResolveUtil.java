/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.resolve2;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.rust.lang.core.psi.ext.RsMod;

public final class FacadeResolveUtil {
    private FacadeResolveUtil() {}

    @Nullable
    public static RsModInfo getModInfo(@Nonnull RsMod mod) {
        return FacadeResolve.getModInfo(mod);
    }
}
