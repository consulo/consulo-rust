/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.psi.ext;

import org.jetbrains.annotations.NotNull;

/**
 * Bridge class delegating to {@link RsAbstractableImplKt} and {@link RsAbstractableOwner}.
 *
 * Some Java files reference {@code RsAbstractableOwnerUtil.getOwner()} and
 * {@code RsAbstractableOwnerUtil.isTraitImpl()}.
 */
public final class RsAbstractableOwnerUtil {

    private RsAbstractableOwnerUtil() {
    }

    @NotNull
    public static RsAbstractableOwner getOwner(@NotNull RsAbstractable abstractable) {
        return RsAbstractableImplUtil.getOwner(abstractable);
    }

    public static boolean isInherentImpl(@NotNull RsAbstractableOwner owner) {
        return owner.isInherentImpl();
    }

    public static boolean isTraitImpl(@NotNull RsAbstractableOwner owner) {
        return owner.isTraitImpl();
    }

    public static boolean isImplOrTrait(@NotNull RsAbstractableOwner owner) {
        return owner.isImplOrTrait();
    }
}
