/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.psi.ext;

import com.intellij.util.ProcessingContext;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.rust.lang.core.psi.*;

/**
 * The real implementations live in {@link RsMetaItemUtil}.
 */
public final class RsMetaItemExtUtil {
    private RsMetaItemExtUtil() {}

    @Nullable
    public static String getId(@NotNull RsMetaItem metaItem) {
        return RsMetaItemUtil.getId(metaItem);
    }

    @Nullable
    public static RsDocAndAttributeOwner getOwner(@NotNull RsMetaItem metaItem) {
        return RsMetaItemUtil.getOwner(metaItem);
    }

    @NotNull
    public static AttributeTemplateType getTemplateType(@NotNull RsMetaItem metaItem) {
        return RsMetaItemUtil.getTemplateType(metaItem);
    }

    public static boolean isRootMetaItem(@NotNull RsMetaItem metaItem, @Nullable ProcessingContext context) {
        return RsMetaItemUtil.isRootMetaItem(metaItem, context);
    }
}
