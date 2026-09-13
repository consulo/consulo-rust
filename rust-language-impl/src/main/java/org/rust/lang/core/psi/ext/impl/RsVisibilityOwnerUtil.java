/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.psi.ext.impl;

import jakarta.annotation.Nonnull;

import consulo.ui.image.Image;
import org.rust.lang.core.psi.ext.impl.RsVisibilityUtil;
import org.rust.lang.core.psi.ext.*;

/**
 * Bridge class delegating to {@link RsVisibilityKt}.
 */
public final class RsVisibilityOwnerUtil {
    private RsVisibilityOwnerUtil() {
    }

    @Nonnull
    public static Image iconWithVisibility(@Nonnull RsVisibilityOwner owner, int flags, @Nonnull Image icon) {
        return RsVisibilityUtil.iconWithVisibility(owner, flags, icon);
    }
}
