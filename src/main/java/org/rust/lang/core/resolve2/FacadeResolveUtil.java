/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.resolve2;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.rust.lang.core.psi.ext.RsMod;

public final class FacadeResolveUtil {
    private FacadeResolveUtil() {}

    @Nullable
    public static RsModInfo getModInfo(@NotNull RsMod mod) {
        return FacadeResolve.getModInfo(mod);
    }
}
