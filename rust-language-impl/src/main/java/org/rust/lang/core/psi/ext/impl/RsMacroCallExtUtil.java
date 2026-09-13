/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.psi.ext.impl;

import consulo.document.util.TextRange;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.rust.lang.core.psi.MacroBraces;
import org.rust.lang.core.psi.RsExpr;
import org.rust.lang.core.psi.RsMacroCall;
import org.rust.stdext.HashCode;
import org.rust.lang.core.psi.ext.*;

/**
 * Delegates to {@link RsMacroCallKt} for backward compatibility.
 */
public final class RsMacroCallExtUtil {
    private RsMacroCallExtUtil() {
    }

    @Nonnull
    public static String getMacroName(@Nonnull RsMacroCall macroCall) {
        return RsMacroCallUtil.getMacroName(macroCall);
    }

    public static boolean isTopLevelExpansion(@Nonnull RsMacroCall macroCall) {
        return RsMacroCallUtil.isTopLevelExpansion(macroCall);
    }

    @Nullable
    public static MacroBraces getBracesKind(@Nonnull RsMacroCall macroCall) {
        return RsMacroCallUtil.getBracesKind(macroCall);
    }

    @Nullable
    public static String getMacroBody(@Nonnull RsMacroCall macroCall) {
        return RsMacroCallUtil.getMacroBody(macroCall);
    }

    @Nullable
    public static TextRange getBodyTextRange(@Nonnull RsMacroCall macroCall) {
        return RsMacroCallUtil.getBodyTextRange(macroCall);
    }

    @Nullable
    public static RsElement getMacroArgumentElement(@Nonnull RsMacroCall macroCall) {
        return RsMacroCallUtil.getMacroArgumentElement(macroCall);
    }

    @Nullable
    public static HashCode getBodyHash(@Nonnull RsMacroCall macroCall) {
        return RsMacroCallUtil.getBodyHash(macroCall);
    }

    @Nullable
    public static RsMacroDefinitionBase resolveToMacro(@Nonnull RsMacroCall macroCall) {
        return RsMacroCallUtil.resolveToMacro(macroCall);
    }

    @Nonnull
    public static RsElement replaceWithExpr(@Nonnull RsMacroCall macroCall, @Nonnull RsExpr expr) {
        return RsMacroCallUtil.replaceWithExpr(macroCall, expr);
    }

    public static boolean isStdTryMacro(@Nonnull RsMacroCall macroCall) {
        return RsMacroCallUtil.isStdTryMacro(macroCall);
    }
}
