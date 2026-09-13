/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.psi.ext.impl;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.rust.lang.core.psi.*;
import org.rust.lang.core.psi.impl.RsRawIdentifiers;
import org.rust.lang.core.psi.ext.*;

public final class RsNamedElementUtil {
    private RsNamedElementUtil() {}

    @Nullable
    public static String getEscapedName(@Nonnull RsNamedElement element) {
        String name = element.getName();
        if (name == null) return null;
        return org.rust.lang.core.psi.impl.RsRawIdentifiers.escapeIdentifierIfNeeded(name);
    }
}
