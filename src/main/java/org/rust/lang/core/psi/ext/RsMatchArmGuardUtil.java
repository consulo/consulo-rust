/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.psi.ext;

import org.jetbrains.annotations.NotNull;
import org.rust.lang.core.psi.RsMatchArm;
import org.rust.lang.core.psi.RsMatchArmGuard;

public final class RsMatchArmGuardUtil {
    private RsMatchArmGuardUtil() {
    }

    @NotNull
    public static RsMatchArm getParentMatchArm(@NotNull RsMatchArmGuard guard) {
        return (RsMatchArm) guard.getParent();
    }
}
