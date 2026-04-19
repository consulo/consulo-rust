/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.macros;

import org.jetbrains.annotations.NotNull;
import org.rust.lang.core.macros.errors.MacroExpansionError;
import org.rust.stdext.RsResult;
import com.intellij.openapi.util.Pair;

public abstract class MacroExpander<T extends RsMacroData, E extends MacroExpansionError> {
    @NotNull
    public abstract RsResult<Pair<CharSequence, RangeMap>, E> expandMacroAsTextWithErr(
        @NotNull T def,
        @NotNull RsMacroCallData call
    );
}
