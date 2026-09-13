/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.psi.ext.impl;

import jakarta.annotation.Nonnull;
import org.rust.lang.core.psi.*;
import org.rust.lang.core.psi.RsMetaItem;
import org.rust.lang.core.psi.ext.*;

public final class RsAttrProcMacroOwnerExtUtil {
    private RsAttrProcMacroOwnerExtUtil() {}

    @Nonnull
    public static QueryAttributes<org.rust.lang.core.psi.RsMetaItem> getQueryAttributes(@Nonnull RsDocAndAttributeOwner owner) {
        return RsDocAndAttributeOwnerUtil.getQueryAttributes(owner);
    }
}
