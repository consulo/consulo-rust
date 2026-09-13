/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.psi.ext.impl;

import jakarta.annotation.Nonnull;
import org.rust.lang.core.psi.*;
import org.rust.lang.core.psi.ext.*;

public final class RsMacroExtUtil {
    private RsMacroExtUtil() {}

    public static boolean getHasRustcBuiltinMacro(@Nonnull RsMacro macro) {
        return macro.getHasRustcBuiltinMacro();
    }
}
