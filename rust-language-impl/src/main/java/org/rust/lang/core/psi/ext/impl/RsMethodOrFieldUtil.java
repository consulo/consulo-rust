/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.psi.ext.impl;

import jakarta.annotation.Nonnull;
import org.rust.lang.core.psi.RsDotExpr;
import org.rust.lang.core.psi.RsExpr;
import org.rust.lang.core.psi.ext.*;

public final class RsMethodOrFieldUtil {
    private RsMethodOrFieldUtil() {
    }

    @Nonnull
    public static RsDotExpr getParentDotExpr(@Nonnull RsMethodOrField methodOrField) {
        return (RsDotExpr) methodOrField.getParent();
    }

    @Nonnull
    public static RsExpr getReceiver(@Nonnull RsMethodOrField methodOrField) {
        return getParentDotExpr(methodOrField).getExpr();
    }
}
