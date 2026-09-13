/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.psi.ext.impl;

import jakarta.annotation.Nonnull;
import org.rust.lang.core.psi.ext.*;

/**
 * Bridge class delegating to {@link RsAbstractableImplKt} and {@link RsAbstractableOwner}.
 *
 * Some Java files reference {@code RsAbstractableOwnerUtil.getOwner()} and
 * {@code RsAbstractableOwnerUtil.isTraitImpl()}.
 */
public final class RsAbstractableOwnerUtil {

    private RsAbstractableOwnerUtil() {
    }

    @Nonnull
    public static RsAbstractableOwner getOwner(@Nonnull RsAbstractable abstractable) {
        return RsAbstractableImplUtil.getOwner(abstractable);
    }

    public static boolean isInherentImpl(@Nonnull RsAbstractableOwner owner) {
        return owner.isInherentImpl();
    }

    public static boolean isTraitImpl(@Nonnull RsAbstractableOwner owner) {
        return owner.isTraitImpl();
    }

    public static boolean isImplOrTrait(@Nonnull RsAbstractableOwner owner) {
        return owner.isImplOrTrait();
    }
}
