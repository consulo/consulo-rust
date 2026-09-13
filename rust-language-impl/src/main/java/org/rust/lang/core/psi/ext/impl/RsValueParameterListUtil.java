/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.psi.ext.impl;

import jakarta.annotation.Nonnull;
import org.rust.lang.core.psi.RsFunction;
import org.rust.lang.core.psi.RsValueParameter;
import org.rust.lang.core.psi.RsValueParameterList;

import java.util.Collections;
import java.util.List;
import org.rust.lang.core.psi.ext.*;

public final class RsValueParameterListUtil {
    private RsValueParameterListUtil() {
    }

    /**
     * Gets the list of value parameters from a function.
     * that returned List<RsValueParameter>.
     */
    @Nonnull
    public static List<RsValueParameter> getValueParameterList(@Nonnull RsFunction function) {
        RsValueParameterList paramList = function.getValueParameterList();
        if (paramList == null) return Collections.emptyList();
        return paramList.getValueParameterList();
    }
}
