/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.psi.ext;

import org.jetbrains.annotations.NotNull;
import org.rust.lang.core.psi.RsPat;
import org.rust.lang.core.psi.RsPatBinding;
import org.rust.lang.core.types.ty.Mutability;

/**
 * Delegates to {@link RsPatBindingKt} for backward compatibility.
 */
public final class RsPatBindingExtUtil {
    private RsPatBindingExtUtil() {
    }

    @NotNull
    public static Mutability getMutability(@NotNull RsPatBinding binding) {
        return RsPatBindingUtil.getMutability(binding);
    }

    public static boolean isArg(@NotNull RsPatBinding binding) {
        return RsPatBindingUtil.isArg(binding);
    }

    @NotNull
    public static RsBindingModeKind getKind(@NotNull RsPatBinding binding) {
        return RsPatBindingUtil.getKind(binding);
    }

    @NotNull
    public static RsPat getTopLevelPattern(@NotNull RsPatBinding binding) {
        return RsPatBindingUtil.getTopLevelPattern(binding);
    }

    public static boolean isReferenceToConstant(@NotNull RsPatBinding binding) {
        return RsPatBindingUtil.isReferenceToConstant(binding);
    }
}
