/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.psi.ext;

import org.jetbrains.annotations.NotNull;
import org.rust.lang.core.psi.RsExternCrateItem;
import org.rust.lang.core.stubs.RsExternCrateItemStub;
import org.rust.lang.core.stubs.RsFunctionStub;

public final class RsExternCrateItemUtil {
    private RsExternCrateItemUtil() {
    }

    @NotNull
    public static String getNameWithAlias(@NotNull RsExternCrateItem item) {
        if (item.getAlias() != null && item.getAlias().getName() != null) {
            return item.getAlias().getName();
        }
        return item.getReferenceName();
    }

    @NotNull
    public static String getNameWithAlias(@NotNull RsExternCrateItemStub stub) {
        if (stub.getAlias() != null && stub.getAlias().getName() != null) {
            return stub.getAlias().getName();
        }
        return stub.getName();
    }

    public static boolean getHasMacroUse(@NotNull RsExternCrateItem item) {
        return EXTERN_CRATE_HAS_MACRO_USE_PROP.getByPsi(item);
    }

    @NotNull
    public static final StubbedAttributeProperty<RsExternCrateItem, RsExternCrateItemStub> EXTERN_CRATE_HAS_MACRO_USE_PROP =
        new StubbedAttributeProperty<>(
            attrs -> attrs.hasAttribute("macro_use"),
            RsExternCrateItemStub::getMayHaveMacroUse
        );
}
