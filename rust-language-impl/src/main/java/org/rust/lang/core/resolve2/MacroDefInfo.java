/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.resolve2;

import jakarta.annotation.Nonnull;
import org.rust.lang.core.psi.impl.RsProcMacroKind;

public abstract class MacroDefInfo {
    public abstract int getCrate();

    @Nonnull
    public abstract ModPath getPath();

    @Nonnull
    public RsProcMacroKind getProcMacroKind() {
        return RsProcMacroKind.FUNCTION_LIKE;
    }
}
