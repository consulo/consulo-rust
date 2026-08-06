/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.resolve2.util;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.rust.lang.core.crate.CratePersistentId;
import org.rust.lang.core.macros.RangeMap;
import org.rust.lang.core.macros.decl.DeclMacroConstantsUtil;
import org.rust.lang.core.resolve2.CrateDefMap;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/**
 * Helper for resolving $crate in macro expansions.
 */
public class DollarCrateHelper {
    @Nonnull
    private final RangeMap ranges;
    @Nonnull
    private final Map<Integer, Integer> rangesInExpansion;
    private final boolean defHasLocalInnerMacros;
    private final int defCrate;

    public DollarCrateHelper(
        @Nonnull RangeMap ranges,
        @Nonnull Map<Integer, Integer> rangesInExpansion,
        boolean defHasLocalInnerMacros,
        int defCrate
    ) {
        this.ranges = ranges;
        this.rangesInExpansion = rangesInExpansion;
        this.defHasLocalInnerMacros = defHasLocalInnerMacros;
        this.defCrate = defCrate;
    }

    @Nonnull
    public String[] convertPath(@Nonnull String[] path, int offsetInExpansion) {
        if (path[0].equals(DeclMacroConstantsUtil.MACRO_DOLLAR_CRATE_IDENTIFIER)) {
            Integer crateId = rangesInExpansion.get(offsetInExpansion);
            if (crateId != null) {
                String[] result = new String[path.length + 1];
                result[0] = DeclMacroConstantsUtil.MACRO_DOLLAR_CRATE_IDENTIFIER;
                result[1] = String.valueOf(crateId);
                System.arraycopy(path, 1, result, 2, path.length - 1);
                return result;
            }
            CrateDefMap.RESOLVE_LOG.error("Can't find crate for path starting with $crate");
            return path;
        }

        if (defHasLocalInnerMacros) {
            Integer pathOffsetInCall = ranges.mapOffsetFromExpansionToCallBody(offsetInExpansion, false);
            if (pathOffsetInCall == null) {
                // Expanded from def
                String[] result = new String[path.length + 2];
                result[0] = DeclMacroConstantsUtil.MACRO_DOLLAR_CRATE_IDENTIFIER;
                result[1] = String.valueOf(defCrate);
                System.arraycopy(path, 0, result, 2, path.length);
                return result;
            }
        }
        return path;
    }

    @Nonnull
    public DollarCrateMap getDollarCrateMap(int startOffsetInExpansion, int endOffsetInExpansion) {
        Map<Integer, Integer> rangesInMacro = new HashMap<>();
        int dollarCrateLen = DeclMacroConstantsUtil.MACRO_DOLLAR_CRATE_IDENTIFIER.length();
        for (Map.Entry<Integer, Integer> entry : rangesInExpansion.entrySet()) {
            int rangeStart = entry.getKey();
            int rangeEnd = rangeStart + dollarCrateLen;
            if (startOffsetInExpansion <= rangeStart && rangeEnd <= endOffsetInExpansion) {
                rangesInMacro.put(rangeStart - startOffsetInExpansion, entry.getValue());
            }
        }
        return new DollarCrateMap(rangesInMacro);
    }
}
