/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.psi.ext;

import jakarta.annotation.Nonnull;
import org.rust.lang.core.psi.*;

public final class RsAttrProcMacroOwnerExtUtil {
    private RsAttrProcMacroOwnerExtUtil() {}

    @Nonnull
    public static QueryAttributes<org.rust.lang.core.psi.RsMetaItem> getQueryAttributes(@Nonnull RsDocAndAttributeOwner owner) {
        return RsDocAndAttributeOwnerUtil.getQueryAttributes(owner);
    }
}
