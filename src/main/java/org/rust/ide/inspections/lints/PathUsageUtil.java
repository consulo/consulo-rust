/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.inspections.lints;

import org.jetbrains.annotations.NotNull;
import org.rust.lang.core.psi.ext.RsItemsOwner;

/**
 * Bridge class that delegates to {@link RsPathUsageAnalysis}.
 * <p>
 * {@code RsItemsOwner.pathUsage}. Now delegates to the hand-written Java conversion.
 */
public final class PathUsageUtil {
    private PathUsageUtil() {
    }

    @NotNull
    public static PathUsageMap getPathUsage(@NotNull RsItemsOwner owner) {
        return RsPathUsageAnalysis.getPathUsage(owner);
    }
}
