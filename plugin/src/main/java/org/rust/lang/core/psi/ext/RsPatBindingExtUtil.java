/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.psi.ext;

import jakarta.annotation.Nonnull;
import org.rust.lang.core.psi.RsPat;
import org.rust.lang.core.psi.RsPatBinding;
import org.rust.lang.core.types.ty.Mutability;

/**
 * Delegates to {@link RsPatBindingKt} for backward compatibility.
 */
public final class RsPatBindingExtUtil {
    private RsPatBindingExtUtil() {
    }

    @Nonnull
    public static Mutability getMutability(@Nonnull RsPatBinding binding) {
        return RsPatBindingUtil.getMutability(binding);
    }

    public static boolean isArg(@Nonnull RsPatBinding binding) {
        return RsPatBindingUtil.isArg(binding);
    }

    @Nonnull
    public static RsBindingModeKind getKind(@Nonnull RsPatBinding binding) {
        return RsPatBindingUtil.getKind(binding);
    }

    @Nonnull
    public static RsPat getTopLevelPattern(@Nonnull RsPatBinding binding) {
        return RsPatBindingUtil.getTopLevelPattern(binding);
    }

    public static boolean isReferenceToConstant(@Nonnull RsPatBinding binding) {
        return RsPatBindingUtil.isReferenceToConstant(binding);
    }
}
