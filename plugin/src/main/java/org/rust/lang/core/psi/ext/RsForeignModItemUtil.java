/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.psi.ext;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.rust.lang.core.psi.RsForeignModItem;

public final class RsForeignModItemUtil {
    private RsForeignModItemUtil() {
    }

    @Nonnull
    public static String getEffectiveAbi(@Nonnull RsForeignModItem item) {
        String abi = getAbi(item);
        return abi != null ? abi : "C";
    }

    @Nullable
    public static String getAbi(@Nonnull RsForeignModItem item) {
        Object stub = RsPsiJavaUtil.getGreenStub(item);
        if (stub != null) {
            return ((org.rust.lang.core.stubs.RsForeignModStub) stub).getAbi();
        }
        if (item.getExternAbi() != null && item.getExternAbi().getLitExpr() != null) {
            return RsLitExprUtil.getStringValue(item.getExternAbi().getLitExpr());
        }
        return null;
    }
}
