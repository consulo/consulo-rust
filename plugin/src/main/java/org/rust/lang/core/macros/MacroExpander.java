/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.macros;

import jakarta.annotation.Nonnull;
import org.rust.lang.core.macros.errors.MacroExpansionError;
import org.rust.stdext.RsResult;
import consulo.util.lang.Pair;

public abstract class MacroExpander<T extends RsMacroData, E extends MacroExpansionError> {
    @Nonnull
    public abstract RsResult<Pair<CharSequence, RangeMap>, E> expandMacroAsTextWithErr(
        @Nonnull T def,
        @Nonnull RsMacroCallData call
    );
}
