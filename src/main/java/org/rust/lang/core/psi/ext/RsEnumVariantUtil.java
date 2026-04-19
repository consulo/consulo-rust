/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.psi.ext;

import org.rust.lang.core.psi.ext.PsiElementUtil;
import org.jetbrains.annotations.NotNull;
import org.rust.lang.core.psi.RsEnumItem;
import org.rust.lang.core.psi.RsEnumVariant;

public final class RsEnumVariantUtil {
    private RsEnumVariantUtil() {
    }

    @NotNull
    public static RsEnumItem getParentEnum(@NotNull RsEnumVariant variant) {
        RsEnumItem parent = PsiElementUtil.stubAncestorStrict(variant, RsEnumItem.class);
        if (parent == null) {
            throw new IllegalStateException("RsEnumVariant must have parent RsEnumItem: " + variant.getText());
        }
        return parent;
    }

    /**
     * Delegates to {@link RsFieldsOwnerExtKt#isFieldless(RsFieldsOwner)}.
     * Provided here for compatibility with callers that use {@code RsEnumVariantUtil.isFieldless}.
     */
    public static boolean isFieldless(@NotNull RsEnumVariant variant) {
        return RsFieldsOwnerExtUtil.isFieldless(variant);
    }
}
