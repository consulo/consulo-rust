/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.resolve2;

import org.jetbrains.annotations.NotNull;
import org.rust.lang.core.psi.RsProcMacroKind;

public abstract class MacroDefInfo {
    public abstract int getCrate();

    @NotNull
    public abstract ModPath getPath();

    @NotNull
    public RsProcMacroKind getProcMacroKind() {
        return RsProcMacroKind.FUNCTION_LIKE;
    }
}
