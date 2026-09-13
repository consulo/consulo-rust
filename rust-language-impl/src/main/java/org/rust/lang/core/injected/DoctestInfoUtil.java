/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.injected;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.rust.lang.doc.psi.RsDocCodeFence;

/**
 * Bridge class delegating to {@link DoctestInfo}.
 */
public final class DoctestInfoUtil {
    private DoctestInfoUtil() {
    }

    @Nullable
    public static DoctestInfo doctestInfo(@Nonnull RsDocCodeFence codeFence) {
        return DoctestInfo.fromCodeFence(codeFence);
    }
}
