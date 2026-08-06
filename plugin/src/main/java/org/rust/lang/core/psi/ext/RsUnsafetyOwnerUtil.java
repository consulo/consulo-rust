/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.psi.ext;

import consulo.language.psi.PsiElement;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

/**
 * Utility class for {@link RsUnsafetyOwner}.
 */
public final class RsUnsafetyOwnerUtil {
    private RsUnsafetyOwnerUtil() {
    }

    @Nullable
    public static PsiElement getUnsafe(@Nonnull RsUnsafetyOwner owner) {
        return owner.getUnsafe();
    }

    public static boolean isUnsafe(@Nonnull RsUnsafetyOwner owner) {
        return owner.isUnsafe();
    }
}
