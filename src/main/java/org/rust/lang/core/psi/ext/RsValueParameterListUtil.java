/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.psi.ext;

import org.jetbrains.annotations.NotNull;
import org.rust.lang.core.psi.RsFunction;
import org.rust.lang.core.psi.RsValueParameter;
import org.rust.lang.core.psi.RsValueParameterList;

import java.util.Collections;
import java.util.List;

public final class RsValueParameterListUtil {
    private RsValueParameterListUtil() {
    }

    /**
     * Gets the list of value parameters from a function.
     * that returned List<RsValueParameter>.
     */
    @NotNull
    public static List<RsValueParameter> getValueParameterList(@NotNull RsFunction function) {
        RsValueParameterList paramList = function.getValueParameterList();
        if (paramList == null) return Collections.emptyList();
        return paramList.getValueParameterList();
    }
}
