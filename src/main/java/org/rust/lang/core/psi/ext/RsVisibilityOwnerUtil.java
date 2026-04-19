/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.psi.ext;

import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import org.rust.lang.core.psi.ext.RsVisibilityUtil;

/**
 * Bridge class delegating to {@link RsVisibilityKt}.
 */
public final class RsVisibilityOwnerUtil {
    private RsVisibilityOwnerUtil() {
    }

    @NotNull
    public static Icon iconWithVisibility(@NotNull RsVisibilityOwner owner, int flags, @NotNull Icon icon) {
        return RsVisibilityUtil.iconWithVisibility(owner, flags, icon);
    }
}
