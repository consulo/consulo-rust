/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.types;

import jakarta.annotation.Nonnull;
import org.rust.lang.core.psi.RsTypeReference;
import org.rust.lang.core.types.ty.Ty;

/**
 * Utility for getting raw types from type references.
 */
public final class RawTypeUtil {
    private RawTypeUtil() {
    }

    @Nonnull
    public static Ty getRawType(@Nonnull RsTypeReference typeRef) {
        return ExtensionsUtil.getRawType(typeRef);
    }
}
