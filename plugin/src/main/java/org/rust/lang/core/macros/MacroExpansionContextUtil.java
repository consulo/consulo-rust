/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.macros;

import jakarta.annotation.Nonnull;
import org.rust.lang.core.psi.ext.RsPossibleMacroCall;
import org.rust.lang.core.psi.ext.RsPossibleMacroCallUtil;

/**
 * Bridge class delegating to {@link MacroExpansionContext}.
 */
public final class MacroExpansionContextUtil {
    private MacroExpansionContextUtil() {
    }

    @Nonnull
    public static MacroExpansionContext getExpansionContext(@Nonnull RsPossibleMacroCall call) {
        return RsPossibleMacroCallUtil.getExpansionContext(call);
    }

    public static boolean isExprOrStmtContext(@Nonnull RsPossibleMacroCall call) {
        MacroExpansionContext ctx = getExpansionContext(call);
        return ctx == MacroExpansionContext.EXPR || ctx == MacroExpansionContext.STMT;
    }
}
