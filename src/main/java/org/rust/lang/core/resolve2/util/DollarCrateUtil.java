/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.resolve2.util;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.rust.lang.core.crate.CratePersistentId;
import org.rust.lang.core.macros.ExpansionResultOk;
import org.rust.lang.core.macros.MacroCallBody;
import org.rust.lang.core.macros.RangeMap;
import org.rust.lang.core.macros.decl.DeclMacroConstantsUtil;
import org.rust.lang.core.resolve2.CrateDefMap;
import org.rust.lang.core.resolve2.DeclMacroDefInfo;
import org.rust.lang.core.resolve2.MacroCallInfo;
import org.rust.lang.core.resolve2.MacroDefInfo;
import org.rust.openapiext.OpenApiUtil;

import java.util.HashMap;
import java.util.Map;

/**
 * <p>
 * Algorithm: for each DeclMacroDefInfo and MacroCallInfo maintain map
 * from index of MACRO_DOLLAR_CRATE_IDENTIFIER occurrence in text to corresponding CratePersistentId.
 * When expanding a macro call, we want for each occurrence
 * of MACRO_DOLLAR_CRATE_IDENTIFIER in expansion.text to find the corresponding CratePersistentId.
 * <p>
 * MACRO_DOLLAR_CRATE_IDENTIFIER could come from:
 * - '$crate' from macro itself (macro_rules) - use DeclMacroDefInfo.crate
 * - MACRO_DOLLAR_CRATE_IDENTIFIER from macro itself (macro_rules) - use map from DeclMacroDefInfo
 * - MACRO_DOLLAR_CRATE_IDENTIFIER from macro call - use map from MacroCallInfo
 */
public final class DollarCrateUtil {

    private DollarCrateUtil() {}

    /**
     * Creates a DollarCrateHelper for a macro expansion.
     *
     * @param call      the macro call info
     * @param def       the macro definition info
     * @param expansion the expansion result
     * @return a DollarCrateHelper, or null if no dollar crate handling is needed
     */
    @Nullable
    public static DollarCrateHelper createDollarCrateHelper(
        @NotNull MacroCallInfo call,
        @NotNull MacroDefInfo def,
        @NotNull ExpansionResultOk expansion
    ) {
        Map<Integer, Integer> rangesInFile = findCrateIdForEachDollarCrate(expansion, call, def.getCrate());
        boolean hasLocalInnerMacros = def instanceof DeclMacroDefInfo && ((DeclMacroDefInfo) def).isHasLocalInnerMacros();
        if (rangesInFile.isEmpty() && !hasLocalInnerMacros) return null;
        return new DollarCrateHelper(expansion.getRanges(), rangesInFile, hasLocalInnerMacros, def.getCrate());
    }

    /**
     * Entry (index, crateId) in returning map means that
     * expansion.text starting from index contains MACRO_DOLLAR_CRATE_IDENTIFIER which corresponds to crateId.
     */
    @NotNull
    private static Map<Integer, Integer> findCrateIdForEachDollarCrate(
        @NotNull ExpansionResultOk expansion,
        @NotNull MacroCallInfo call,
        int defCrate
    ) {
        RangeMap ranges = expansion.getRanges();
        Map<Integer, Integer> result = new HashMap<>();

        for (int indexInExpandedText : expansion.getDollarCrateOccurrences()) {
            Integer indexInCallBody = ranges.mapOffsetFromExpansionToCallBody(indexInExpandedText, false);
            Integer crateId;
            if (indexInCallBody != null) {
                if (call.getBody() instanceof MacroCallBody.FunctionLike) {
                    MacroCallBody.FunctionLike functionLikeBody = (MacroCallBody.FunctionLike) call.getBody();
                    OpenApiUtil.testAssert(() -> {
                        String fragmentInCallBody = functionLikeBody.getText().subSequence(
                            indexInCallBody,
                            indexInCallBody + DeclMacroConstantsUtil.MACRO_DOLLAR_CRATE_IDENTIFIER.length()
                        ).toString();
                        return fragmentInCallBody.equals(DeclMacroConstantsUtil.MACRO_DOLLAR_CRATE_IDENTIFIER);
                    });
                }
                crateId = call.getDollarCrateMap().mapOffsetFromExpansionToCallBody(indexInCallBody);
                if (crateId == null) {
                    CrateDefMap.RESOLVE_LOG.error(
                        "Unexpected macro expansion. Macro call: '" + call + "', expansion: '" + expansion.getText() + "'"
                    );
                    continue;
                }
            } else {
                // $crate came from macro definition body
                crateId = defCrate;
            }
            result.put(indexInExpandedText, crateId);
        }

        return result;
    }
}
