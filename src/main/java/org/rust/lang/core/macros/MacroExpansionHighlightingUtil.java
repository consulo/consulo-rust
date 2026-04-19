/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.macros;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.rust.lang.core.psi.ProcMacroAttribute;
import org.rust.lang.core.psi.RsMetaItem;

/**
 * Bridge between {@link ProcMacroAttribute} and the {@link MacroHighlightingUtil#prepareForExpansionHighlighting}
 * extension function on {@code RsPossibleMacroCall}. Mirrors {@code procMacroAttribute.attr.prepareForExpansionHighlighting(...)}
 */
public final class MacroExpansionHighlightingUtil {
    private MacroExpansionHighlightingUtil() {
    }

    @Nullable
    public static PreparedProcMacroExpansion prepareForExpansionHighlighting(
        @NotNull ProcMacroAttribute<?> attr
    ) {
        return prepareForExpansionHighlighting(attr, null);
    }

    @Nullable
    public static PreparedProcMacroExpansion prepareForExpansionHighlighting(
        @NotNull ProcMacroAttribute<?> attr,
        @Nullable PreparedProcMacroExpansion parent
    ) {
        Object meta = attr.getAttr();
        if (!(meta instanceof RsMetaItem)) return null;
        MacroCallPreparedForHighlighting ancestor = parent instanceof PreparedProcMacroExpansionWrapped
            ? ((PreparedProcMacroExpansionWrapped) parent).inner
            : null;
        MacroCallPreparedForHighlighting prepared =
            MacroHighlightingUtil.prepareForExpansionHighlighting((RsMetaItem) meta, ancestor);
        if (prepared == null) return null;
        return new PreparedProcMacroExpansionWrapped(prepared);
    }

    /** Package-private subclass that retains the underlying {@link MacroCallPreparedForHighlighting}. */
    static final class PreparedProcMacroExpansionWrapped extends PreparedProcMacroExpansion {
        final MacroCallPreparedForHighlighting inner;

        PreparedProcMacroExpansionWrapped(@NotNull MacroCallPreparedForHighlighting inner) {
            super(inner.getElementsForErrorHighlighting());
            this.inner = inner;
        }
    }
}
