/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.psi.ext;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.rust.lang.core.crate.Crate;
import org.rust.lang.core.psi.RsMetaItem;
import org.rust.lang.core.psi.RsOuterAttr;

/**
 * Bridge class that re-exports methods from {@link RsDocAndAttributeOwnerKt}.
 * <p>
 * Some callers reference {@code RsDocAndAttributeOwnerExtUtil} instead of
 * {@code RsDocAndAttributeOwnerKt}. This class delegates to the main class.
 */
public final class RsDocAndAttributeOwnerExtUtil {
    private RsDocAndAttributeOwnerExtUtil() {
    }

    @NotNull
    public static QueryAttributes<RsMetaItem> getQueryAttributes(@NotNull RsDocAndAttributeOwner owner) {
        return RsDocAndAttributeOwnerUtil.getQueryAttributes(owner);
    }

    @NotNull
    public static QueryAttributes<RsMetaItem> getQueryAttributes(@NotNull RsDocAndAttributeOwner owner,
                                                                  @Nullable Crate crate) {
        return RsDocAndAttributeOwnerUtil.getQueryAttributes(owner, crate);
    }

    public static boolean isEnabledByCfgSelfOrInAttrProcMacroBody(@NotNull RsDocAndAttributeOwner owner) {
        return RsDocAndAttributeOwnerUtil.isEnabledByCfgSelfOrInAttrProcMacroBody(owner);
    }

    public static boolean isEnabledByCfgSelfOrInAttrProcMacroBody(@NotNull RsDocAndAttributeOwner owner,
                                                                   @Nullable Crate crate) {
        return RsDocAndAttributeOwnerUtil.isEnabledByCfgSelfOrInAttrProcMacroBody(owner, crate);
    }

    @Nullable
    public static RsOuterAttr findOuterAttr(@NotNull RsOuterAttributeOwner owner, @NotNull String name) {
        return RsDocAndAttributeOwnerUtil.findOuterAttr(owner, name);
    }

    @NotNull
    public static QueryAttributes<RsMetaItem> getTraversedRawAttributes(@NotNull RsDocAndAttributeOwner owner,
                                                                        boolean withCfgAttrAttribute) {
        return RsDocAndAttributeOwnerUtil.getTraversedRawAttributes(owner, withCfgAttrAttribute);
    }

    public static boolean existsAfterExpansionSelf(@NotNull RsDocAndAttributeOwner owner, @Nullable Crate crate) {
        return RsDocAndAttributeOwnerUtil.existsAfterExpansionSelf(owner, crate);
    }

    public static boolean isEnabledByCfgSelf(@NotNull RsDocAndAttributeOwner owner, @Nullable Crate crate) {
        return RsDocAndAttributeOwnerUtil.isEnabledByCfgSelf(owner, crate);
    }

    public static boolean isCfgUnknownSelf(@NotNull RsDocAndAttributeOwner owner) {
        return RsDocAndAttributeOwnerUtil.isCfgUnknownSelf(owner);
    }
}
