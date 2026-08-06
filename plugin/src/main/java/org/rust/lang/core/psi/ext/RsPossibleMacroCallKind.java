/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.psi.ext;

import jakarta.annotation.Nonnull;
import org.rust.lang.core.psi.RsMacroCall;
import org.rust.lang.core.psi.RsMetaItem;

public abstract class RsPossibleMacroCallKind {
    private RsPossibleMacroCallKind() {}

    public static final class MacroCall extends RsPossibleMacroCallKind {
        @Nonnull public final RsMacroCall call;
        public MacroCall(@Nonnull RsMacroCall call) { this.call = call; }
    }

    public static final class MetaItem extends RsPossibleMacroCallKind {
        @Nonnull public final RsMetaItem meta;
        public MetaItem(@Nonnull RsMetaItem meta) { this.meta = meta; }
    }
}
