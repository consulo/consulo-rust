/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.types.ty;
import consulo.util.lang.Pair;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.rust.lang.core.resolve.KnownItems;

/**
 * Bridge class delegating to {@link TyUtil}.
 */
public final class TyUtilUtil {
    private TyUtilUtil() {
    }

    public static Ty builtinIndex(@Nonnull Ty ty) {
        return TyUtil.builtinIndex(ty);
    }

    public static Pair<Ty, Mutability> builtinDeref(@Nonnull Ty ty, @Nullable KnownItems items, boolean explicit) {
        return TyUtil.builtinDeref(ty, items, explicit);
    }

    public static Pair<Ty, Mutability> builtinDeref(@Nonnull Ty ty, @Nullable KnownItems items) {
        return TyUtil.builtinDeref(ty, items);
    }
}
