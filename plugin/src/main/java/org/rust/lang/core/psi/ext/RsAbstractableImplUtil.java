/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.psi.ext;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.rust.lang.core.psi.ext.RsTraitOrImpl;

/**
 * Delegates to {@link RsAbstractableImplUtil} for the actual implementations.
 *
 */
public final class RsAbstractableImplUtil {

    private RsAbstractableImplUtil() {
    }

    @Nonnull
    public static RsAbstractableOwner getOwner(@Nonnull RsAbstractable abstractable) {
        return RsAbstractableImplUtil.getOwner(abstractable);
    }

    @Nonnull
    public static RsAbstractableOwner getOwnerBySyntaxOnly(@Nonnull RsAbstractable abstractable) {
        return RsAbstractableImplUtil.getOwnerBySyntaxOnly(abstractable);
    }

    @Nullable
    public static RsAbstractable getSuperItem(@Nonnull RsAbstractable abstractable) {
        return RsAbstractableImplUtil.getSuperItem(abstractable);
    }

    @Nullable
    public static RsAbstractable findCorrespondingElement(@Nonnull RsTraitOrImpl traitOrImpl,
                                                           @Nonnull RsAbstractable element) {
        return RsAbstractableImplUtil.findCorrespondingElement(traitOrImpl, element);
    }
}
