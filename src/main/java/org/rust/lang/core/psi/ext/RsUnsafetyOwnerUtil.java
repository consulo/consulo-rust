/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.psi.ext;

import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Utility class for {@link RsUnsafetyOwner}.
 */
public final class RsUnsafetyOwnerUtil {
    private RsUnsafetyOwnerUtil() {
    }

    @Nullable
    public static PsiElement getUnsafe(@NotNull RsUnsafetyOwner owner) {
        return owner.getUnsafe();
    }

    public static boolean isUnsafe(@NotNull RsUnsafetyOwner owner) {
        return owner.isUnsafe();
    }
}
