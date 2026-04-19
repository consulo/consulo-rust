/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.mir;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.rust.lang.core.mir.schemas.MirBody;
import org.rust.lang.core.psi.ext.RsInferenceContextOwner;

/**
 * Delegates to {@link MirExtensions} which contains the full implementation.
 */
public final class Extensions {
    private Extensions() {
    }

    /**
     * Extension property mirBody on RsInferenceContextOwner.
     * @see MirExtensions#getMirBody(RsInferenceContextOwner)
     */
    @Nullable
    public static MirBody getMirBody(@NotNull RsInferenceContextOwner owner) {
        return MirExtensions.getMirBody(owner);
    }
}
