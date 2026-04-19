/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.psi.ext;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.rust.lang.core.psi.ext.RsTraitOrImpl;

/**
 * Delegates to {@link RsAbstractableImplUtil} for the actual implementations.
 */
public final class RsAbstractableExtUtil {

    private RsAbstractableExtUtil() {
    }

    @NotNull
    public static RsAbstractableOwner getOwner(@NotNull RsAbstractable abstractable) {
        return RsAbstractableImplUtil.getOwner(abstractable);
    }

    @NotNull
    public static RsAbstractableOwner getOwnerBySyntaxOnly(@NotNull RsAbstractable abstractable) {
        return RsAbstractableImplUtil.getOwnerBySyntaxOnly(abstractable);
    }

    @Nullable
    public static RsAbstractable getSuperItem(@NotNull RsAbstractable abstractable) {
        return RsAbstractableImplUtil.getSuperItem(abstractable);
    }

    @Nullable
    public static RsAbstractable findCorrespondingElement(@NotNull RsTraitOrImpl traitOrImpl,
                                                           @NotNull RsAbstractable element) {
        return RsAbstractableImplUtil.findCorrespondingElement(traitOrImpl, element);
    }
}
