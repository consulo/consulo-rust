/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.annotator.format;

/**
 * Bridge class delegating to {@link FormatParameter}.
 */
public final class FormatParameterUtil {
    private FormatParameterUtil() {
    }

    @org.jetbrains.annotations.Nullable
    public static org.rust.lang.core.psi.ext.RsStructOrEnumItemElement baseType(@org.jetbrains.annotations.NotNull org.rust.lang.core.types.ty.Ty ty) {
        org.rust.lang.core.types.ty.Ty unwrapped = ty;
        while (unwrapped instanceof org.rust.lang.core.types.ty.TyReference) {
            unwrapped = ((org.rust.lang.core.types.ty.TyReference) unwrapped).getReferenced();
        }
        if (unwrapped instanceof org.rust.lang.core.types.ty.TyAdt) {
            return ((org.rust.lang.core.types.ty.TyAdt) unwrapped).getItem();
        }
        return null;
    }
}
