/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.psi.ext;

import org.jetbrains.annotations.NotNull;
import org.rust.lang.core.psi.RsModItem;
import org.rust.lang.core.stubs.RsModItemStub;

public final class RsModItemUtil {
    private RsModItemUtil() {
    }

    public static boolean getHasMacroUse(@NotNull RsModItem modItem) {
        return MOD_ITEM_HAS_MACRO_USE_PROP.getByPsi(modItem);
    }

    @NotNull
    public static final StubbedAttributeProperty<RsModItem, RsModItemStub> MOD_ITEM_HAS_MACRO_USE_PROP =
        new StubbedAttributeProperty<>(qa -> qa.hasAttribute("macro_use"), RsModItemStub::getMayHaveMacroUse);
}
