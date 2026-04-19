/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.psi.ext;

import org.jetbrains.annotations.NotNull;
import org.rust.lang.core.psi.RsDotExpr;
import org.rust.lang.core.psi.RsExpr;

public final class RsMethodOrFieldUtil {
    private RsMethodOrFieldUtil() {
    }

    @NotNull
    public static RsDotExpr getParentDotExpr(@NotNull RsMethodOrField methodOrField) {
        return (RsDotExpr) methodOrField.getParent();
    }

    @NotNull
    public static RsExpr getReceiver(@NotNull RsMethodOrField methodOrField) {
        return getParentDotExpr(methodOrField).getExpr();
    }
}
