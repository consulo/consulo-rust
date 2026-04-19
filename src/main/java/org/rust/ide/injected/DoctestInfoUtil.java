/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.injected;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.rust.lang.doc.psi.RsDocCodeFence;

/**
 * Bridge class delegating to {@link DoctestInfo}.
 */
public final class DoctestInfoUtil {
    private DoctestInfoUtil() {
    }

    @Nullable
    public static DoctestInfo doctestInfo(@NotNull RsDocCodeFence codeFence) {
        return DoctestInfo.fromCodeFence(codeFence);
    }
}
