/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.resolve2;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public final class PathResolutionUtil {
    private PathResolutionUtil() {}

    /**
     * returns the single element whose {@code hasMacroExport} is {@code true}, or the first
     * element if there is no unique such element.
     */
    @Nullable
    @SuppressWarnings("unchecked")
    public static MacroDefInfo singlePublicOrFirstDefInfo(@NotNull Object perNs) {
        List<DeclMacroDefInfo> list = (List<DeclMacroDefInfo>) perNs;
        if (list.isEmpty()) return null;
        DeclMacroDefInfo onlyPublic = null;
        int publicCount = 0;
        for (DeclMacroDefInfo it : list) {
            if (it.isHasMacroExport()) {
                onlyPublic = it;
                publicCount++;
                if (publicCount > 1) break;
            }
        }
        return publicCount == 1 ? onlyPublic : list.get(0);
    }

    /** Delegates to {@link PathResolution#resolveExternCrateAsDefMap}. */
    @Nullable
    public static CrateDefMap resolveExternCrateAsDefMap(@NotNull CrateDefMap defMap, @NotNull String name) {
        return PathResolution.resolveExternCrateAsDefMap(defMap, name);
    }
}
