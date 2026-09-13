/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.psi.ext.impl;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.rust.lang.core.crate.Crate;
import org.rust.lang.core.psi.RsMetaItem;
import org.rust.lang.core.psi.RsOuterAttr;
import org.rust.lang.core.psi.ext.*;

/**
 * Bridge class that re-exports methods from {@link RsDocAndAttributeOwnerKt}.
 * <p>
 * Some callers reference {@code RsDocAndAttributeOwnerExtUtil} instead of
 * {@code RsDocAndAttributeOwnerKt}. This class delegates to the main class.
 */
public final class RsDocAndAttributeOwnerExtUtil {
    private RsDocAndAttributeOwnerExtUtil() {
    }

    @Nonnull
    public static QueryAttributes<RsMetaItem> getQueryAttributes(@Nonnull RsDocAndAttributeOwner owner) {
        return RsDocAndAttributeOwnerUtil.getQueryAttributes(owner);
    }

    @Nonnull
    public static QueryAttributes<RsMetaItem> getQueryAttributes(@Nonnull RsDocAndAttributeOwner owner,
                                                                  @Nullable Crate crate) {
        return RsDocAndAttributeOwnerUtil.getQueryAttributes(owner, crate);
    }

    public static boolean isEnabledByCfgSelfOrInAttrProcMacroBody(@Nonnull RsDocAndAttributeOwner owner) {
        return RsDocAndAttributeOwnerUtil.isEnabledByCfgSelfOrInAttrProcMacroBody(owner);
    }

    public static boolean isEnabledByCfgSelfOrInAttrProcMacroBody(@Nonnull RsDocAndAttributeOwner owner,
                                                                   @Nullable Crate crate) {
        return RsDocAndAttributeOwnerUtil.isEnabledByCfgSelfOrInAttrProcMacroBody(owner, crate);
    }

    @Nullable
    public static RsOuterAttr findOuterAttr(@Nonnull RsOuterAttributeOwner owner, @Nonnull String name) {
        return RsDocAndAttributeOwnerUtil.findOuterAttr(owner, name);
    }

    @Nonnull
    public static QueryAttributes<RsMetaItem> getTraversedRawAttributes(@Nonnull RsDocAndAttributeOwner owner,
                                                                        boolean withCfgAttrAttribute) {
        return RsDocAndAttributeOwnerUtil.getTraversedRawAttributes(owner, withCfgAttrAttribute);
    }

    public static boolean existsAfterExpansionSelf(@Nonnull RsDocAndAttributeOwner owner, @Nullable Crate crate) {
        return RsDocAndAttributeOwnerUtil.existsAfterExpansionSelf(owner, crate);
    }

    public static boolean isEnabledByCfgSelf(@Nonnull RsDocAndAttributeOwner owner, @Nullable Crate crate) {
        return RsDocAndAttributeOwnerUtil.isEnabledByCfgSelf(owner, crate);
    }

    public static boolean isCfgUnknownSelf(@Nonnull RsDocAndAttributeOwner owner) {
        return RsDocAndAttributeOwnerUtil.isCfgUnknownSelf(owner);
    }
}
