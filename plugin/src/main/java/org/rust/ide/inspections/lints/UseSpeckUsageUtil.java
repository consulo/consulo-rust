/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.inspections.lints;

import jakarta.annotation.Nonnull;
import org.rust.lang.core.psi.RsUseSpeck;

public final class UseSpeckUsageUtil {
    private UseSpeckUsageUtil() {
    }

    /**
     * @return whether {@code useSpeck} imports something that is actually referenced, taking the
     * {@code unused_imports} lint level into account.
     */
    public static boolean isUsed(@Nonnull RsUseSpeck useSpeck, @Nonnull PathUsageMap pathUsage) {
        return RsUnusedImportInspection.isUseSpeckUsed(useSpeck, pathUsage);
    }
}
