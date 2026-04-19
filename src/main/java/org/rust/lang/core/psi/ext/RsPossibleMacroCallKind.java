/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.psi.ext;

import org.jetbrains.annotations.NotNull;
import org.rust.lang.core.psi.RsMacroCall;
import org.rust.lang.core.psi.RsMetaItem;

public abstract class RsPossibleMacroCallKind {
    private RsPossibleMacroCallKind() {}

    public static final class MacroCall extends RsPossibleMacroCallKind {
        @NotNull public final RsMacroCall call;
        public MacroCall(@NotNull RsMacroCall call) { this.call = call; }
    }

    public static final class MetaItem extends RsPossibleMacroCallKind {
        @NotNull public final RsMetaItem meta;
        public MetaItem(@NotNull RsMetaItem meta) { this.meta = meta; }
    }
}
