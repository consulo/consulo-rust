/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.psi.ext.impl;

import consulo.language.util.ProcessingContext;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.rust.lang.core.psi.*;
import org.rust.lang.core.psi.ext.*;

/**
 * The real implementations live in {@link RsMetaItemUtil}.
 */
public final class RsMetaItemExtUtil {
    private RsMetaItemExtUtil() {}

    @Nullable
    public static String getId(@Nonnull RsMetaItem metaItem) {
        return RsMetaItemUtil.getId(metaItem);
    }

    @Nullable
    public static RsDocAndAttributeOwner getOwner(@Nonnull RsMetaItem metaItem) {
        return RsMetaItemUtil.getOwner(metaItem);
    }

    @Nonnull
    public static AttributeTemplateType getTemplateType(@Nonnull RsMetaItem metaItem) {
        return RsMetaItemUtil.getTemplateType(metaItem);
    }

    public static boolean isRootMetaItem(@Nonnull RsMetaItem metaItem, @Nullable ProcessingContext context) {
        return RsMetaItemUtil.isRootMetaItem(metaItem, context);
    }
}
