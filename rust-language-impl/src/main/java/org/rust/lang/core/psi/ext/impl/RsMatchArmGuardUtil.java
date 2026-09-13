/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.psi.ext.impl;

import jakarta.annotation.Nonnull;
import org.rust.lang.core.psi.RsMatchArm;
import org.rust.lang.core.psi.RsMatchArmGuard;
import org.rust.lang.core.psi.ext.*;

public final class RsMatchArmGuardUtil {
    private RsMatchArmGuardUtil() {
    }

    @Nonnull
    public static RsMatchArm getParentMatchArm(@Nonnull RsMatchArmGuard guard) {
        return (RsMatchArm) guard.getParent();
    }
}
