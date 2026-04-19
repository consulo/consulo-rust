/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.macros;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.rust.lang.core.psi.RsMacroCall;
import org.rust.lang.core.psi.RsMetaItem;
import org.rust.lang.core.macros.RsExpandedElementUtil;
import org.rust.lang.core.psi.ext.RsPossibleMacroCall;
import org.rust.lang.core.psi.ext.RsPossibleMacroCallUtil;

public final class MacroHighlightingUtil {
    private MacroHighlightingUtil() {
    }

    @Nullable
    public static MacroCallPreparedForHighlighting prepareForExpansionHighlighting(
        @NotNull RsPossibleMacroCall call,
        @Nullable MacroCallPreparedForHighlighting ancestorMacro
    ) {
        if (call instanceof RsMacroCall && ((RsMacroCall) call).getMacroArgument() == null) return null;
        if (!RsPossibleMacroCallUtil.getExistsAfterExpansion(call)) return null;
        MacroExpansion expansion = RsPossibleMacroCallUtil.getExpansion(call);
        if (expansion == null) return null;
        boolean isDeeplyAttrMacro = (ancestorMacro == null || ancestorMacro.isDeeplyAttrMacro()) && call instanceof RsMetaItem;
        return new MacroCallPreparedForHighlighting(call, expansion, isDeeplyAttrMacro);
    }

    @Nullable
    public static MacroCallPreparedForHighlighting prepareForExpansionHighlighting(@NotNull RsPossibleMacroCall call) {
        return prepareForExpansionHighlighting(call, null);
    }
}
