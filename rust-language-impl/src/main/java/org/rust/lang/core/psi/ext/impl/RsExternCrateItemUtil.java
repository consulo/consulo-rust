/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.psi.ext.impl;

import jakarta.annotation.Nonnull;
import org.rust.lang.core.psi.RsExternCrateItem;
import org.rust.lang.core.stubs.RsExternCrateItemStub;
import org.rust.lang.core.psi.ext.*;

public final class RsExternCrateItemUtil {
    private RsExternCrateItemUtil() {
    }

    @Nonnull
    public static String getNameWithAlias(@Nonnull RsExternCrateItem item) {
        if (item.getAlias() != null && item.getAlias().getName() != null) {
            return item.getAlias().getName();
        }
        return item.getReferenceName();
    }

    @Nonnull
    public static String getNameWithAlias(@Nonnull RsExternCrateItemStub stub) {
        if (stub.getAlias() != null && stub.getAlias().getName() != null) {
            return stub.getAlias().getName();
        }
        return stub.getName();
    }

    public static boolean getHasMacroUse(@Nonnull RsExternCrateItem item) {
        return EXTERN_CRATE_HAS_MACRO_USE_PROP.getByPsi(item);
    }

    @Nonnull
    public static final StubbedAttributeProperty<RsExternCrateItem, RsExternCrateItemStub> EXTERN_CRATE_HAS_MACRO_USE_PROP =
        new StubbedAttributeProperty<>(
            attrs -> attrs.hasAttribute("macro_use"),
            RsExternCrateItemStub::getMayHaveMacroUse
        );
}
