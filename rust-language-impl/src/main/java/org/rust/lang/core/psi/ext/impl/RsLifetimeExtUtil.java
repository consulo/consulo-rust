/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.psi.ext.impl;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.rust.lang.core.psi.RsLifetime;
import org.rust.lang.core.psi.ext.*;

public final class RsLifetimeExtUtil {
    private RsLifetimeExtUtil() {}

    /**
     * a lifetime token by its source text.
     */
    @Nonnull
    public static LifetimeName getTypedName(@Nullable RsLifetime lifetime) {
        if (lifetime == null) return LifetimeName.Implicit.INSTANCE;
        String text = lifetime.getReferenceName();
        if (text == null) return LifetimeName.Implicit.INSTANCE;
        if ("'_".equals(text)) return LifetimeName.Underscore.INSTANCE;
        if ("'static".equals(text)) return LifetimeName.Static.INSTANCE;
        return new LifetimeName.Parameter(text);
    }

    /** {@code val RsLifetime?.isElided}. */
    public static boolean isElided(@Nullable RsLifetime lifetime) {
        return getTypedName(lifetime).isElided();
    }
}
