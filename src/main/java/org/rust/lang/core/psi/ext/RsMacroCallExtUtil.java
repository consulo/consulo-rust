/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.psi.ext;

import com.intellij.openapi.util.TextRange;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.rust.lang.core.psi.MacroBraces;
import org.rust.lang.core.psi.RsExpr;
import org.rust.lang.core.psi.RsMacroCall;
import org.rust.stdext.HashCode;

/**
 * Delegates to {@link RsMacroCallKt} for backward compatibility.
 */
public final class RsMacroCallExtUtil {
    private RsMacroCallExtUtil() {
    }

    @NotNull
    public static String getMacroName(@NotNull RsMacroCall macroCall) {
        return RsMacroCallUtil.getMacroName(macroCall);
    }

    public static boolean isTopLevelExpansion(@NotNull RsMacroCall macroCall) {
        return RsMacroCallUtil.isTopLevelExpansion(macroCall);
    }

    @Nullable
    public static MacroBraces getBracesKind(@NotNull RsMacroCall macroCall) {
        return RsMacroCallUtil.getBracesKind(macroCall);
    }

    @Nullable
    public static String getMacroBody(@NotNull RsMacroCall macroCall) {
        return RsMacroCallUtil.getMacroBody(macroCall);
    }

    @Nullable
    public static TextRange getBodyTextRange(@NotNull RsMacroCall macroCall) {
        return RsMacroCallUtil.getBodyTextRange(macroCall);
    }

    @Nullable
    public static RsElement getMacroArgumentElement(@NotNull RsMacroCall macroCall) {
        return RsMacroCallUtil.getMacroArgumentElement(macroCall);
    }

    @Nullable
    public static HashCode getBodyHash(@NotNull RsMacroCall macroCall) {
        return RsMacroCallUtil.getBodyHash(macroCall);
    }

    @Nullable
    public static RsMacroDefinitionBase resolveToMacro(@NotNull RsMacroCall macroCall) {
        return RsMacroCallUtil.resolveToMacro(macroCall);
    }

    @NotNull
    public static RsElement replaceWithExpr(@NotNull RsMacroCall macroCall, @NotNull RsExpr expr) {
        return RsMacroCallUtil.replaceWithExpr(macroCall, expr);
    }

    public static boolean isStdTryMacro(@NotNull RsMacroCall macroCall) {
        return RsMacroCallUtil.isStdTryMacro(macroCall);
    }
}
