/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.types;

import org.jetbrains.annotations.NotNull;
import org.rust.lang.core.psi.RsTypeReference;
import org.rust.lang.core.types.ty.Ty;

/**
 * Utility for getting normalized types from type references.
 */
public final class NormTypeUtil {
    private NormTypeUtil() {
    }

    @NotNull
    public static Ty getNormType(@NotNull RsTypeReference typeRef) {
        return ExtensionsUtil.getNormType(typeRef);
    }
}
