/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.psi.ext;

import jakarta.annotation.Nonnull;
import org.rust.lang.core.psi.RsMatchArm;
import org.rust.lang.core.psi.RsMatchBody;
import org.rust.lang.core.psi.RsMatchExpr;

import java.util.Collections;
import java.util.List;

public final class RsMatchExprUtil {
    private RsMatchExprUtil() {
    }

    @Nonnull
    public static List<RsMatchArm> getArms(@Nonnull RsMatchExpr matchExpr) {
        RsMatchBody body = matchExpr.getMatchBody();
        return body != null ? body.getMatchArmList() : Collections.emptyList();
    }
}
