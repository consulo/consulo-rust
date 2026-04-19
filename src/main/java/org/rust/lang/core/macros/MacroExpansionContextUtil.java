/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.macros;

import org.jetbrains.annotations.NotNull;
import org.rust.lang.core.psi.ext.RsPossibleMacroCall;
import org.rust.lang.core.psi.ext.RsPossibleMacroCallUtil;

/**
 * Bridge class delegating to {@link MacroExpansionContext}.
 */
public final class MacroExpansionContextUtil {
    private MacroExpansionContextUtil() {
    }

    @NotNull
    public static MacroExpansionContext getExpansionContext(@NotNull RsPossibleMacroCall call) {
        return RsPossibleMacroCallUtil.getExpansionContext(call);
    }

    public static boolean isExprOrStmtContext(@NotNull RsPossibleMacroCall call) {
        MacroExpansionContext ctx = getExpansionContext(call);
        return ctx == MacroExpansionContext.EXPR || ctx == MacroExpansionContext.STMT;
    }
}
