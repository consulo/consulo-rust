/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.psi.ext;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.rust.lang.core.psi.RsMetaItem;

/**
 * Utility methods for RsDocAndAttributeOwner extensions.
 */
public final class RsAttrOwnerExtUtil {
    private RsAttrOwnerExtUtil() {
    }

    @Nullable
    public static RsMetaItem findFirstMetaItem(@NotNull RsDocAndAttributeOwner owner, @NotNull String name) {
        QueryAttributes<RsMetaItem> queryAttributes = RsDocAndAttributeOwnerUtil.getQueryAttributes(owner);
        for (RsMetaItem metaItem : queryAttributes.getMetaItems()) {
            if (name.equals(RsMetaItemUtil.getName(metaItem))) {
                return metaItem;
            }
        }
        return null;
    }
}
