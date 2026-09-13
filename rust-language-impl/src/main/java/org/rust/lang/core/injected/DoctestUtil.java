/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.injected;
import org.rust.lang.core.psi.RsFunction;

public final class DoctestUtil {
    private DoctestUtil() {
    }

    public static boolean isDoctestInjectedMain(@org.jetbrains.annotations.NotNull org.rust.lang.core.psi.RsFunction function) {
        return false;
    }
}
