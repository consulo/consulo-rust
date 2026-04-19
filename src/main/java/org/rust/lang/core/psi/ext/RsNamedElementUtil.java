/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.psi.ext;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.rust.lang.core.psi.*;

public final class RsNamedElementUtil {
    private RsNamedElementUtil() {}

    @Nullable
    public static String getEscapedName(@NotNull RsNamedElement element) {
        String name = element.getName();
        if (name == null) return null;
        return org.rust.lang.core.psi.RsRawIdentifiers.escapeIdentifierIfNeeded(name);
    }
}
