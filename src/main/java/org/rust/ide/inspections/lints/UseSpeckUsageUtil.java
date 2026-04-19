/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.inspections.lints;

public final class UseSpeckUsageUtil {
    private UseSpeckUsageUtil() {
    }

    public static boolean isUsed(@org.jetbrains.annotations.NotNull org.rust.lang.core.psi.RsUseSpeck useSpeck, @org.jetbrains.annotations.NotNull PathUsageMap pathUsage) {
        // Stub: assume all use specks are used
        return true;
    }
}
