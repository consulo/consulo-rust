/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.psi.ext;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.rust.lang.core.psi.RsMetaItem;

/**
 * Utility methods for RsDocAndAttributeOwner extensions.
 */
public final class RsAttrOwnerExtUtil {
    private RsAttrOwnerExtUtil() {
    }

    @Nullable
    public static RsMetaItem findFirstMetaItem(@Nonnull RsDocAndAttributeOwner owner, @Nonnull String name) {
        QueryAttributes<RsMetaItem> queryAttributes = RsDocAndAttributeOwnerUtil.getQueryAttributes(owner);
        for (RsMetaItem metaItem : queryAttributes.getMetaItems()) {
            if (name.equals(RsMetaItemUtil.getName(metaItem))) {
                return metaItem;
            }
        }
        return null;
    }
}
