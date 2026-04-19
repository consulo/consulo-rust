/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.psi.ext;

import org.rust.lang.core.psi.ext.PsiElementUtil;
import org.jetbrains.annotations.NotNull;
import org.rust.lang.core.psi.RsConstParameter;
import org.rust.lang.core.psi.RsLifetimeParameter;
import org.rust.lang.core.psi.RsTypeParameter;
import org.rust.lang.core.psi.RsTypeParameterList;

import java.util.ArrayList;
import java.util.List;

public final class RsTypeParameterListUtil {
    private RsTypeParameterListUtil() {
    }

    @NotNull
    public static List<RsGenericParameter> getGenericParameters(@NotNull RsTypeParameterList list) {
        return getGenericParameters(list, true, true, true);
    }

    @NotNull
    public static List<RsGenericParameter> getGenericParameters(
        @NotNull RsTypeParameterList list,
        boolean includeLifetimes
    ) {
        return getGenericParameters(list, includeLifetimes, true, true);
    }

    @NotNull
    public static List<RsGenericParameter> getGenericParameters(
        @NotNull RsTypeParameterList list,
        boolean includeLifetimes,
        boolean includeTypes,
        boolean includeConsts
    ) {
        List<RsGenericParameter> result = new ArrayList<>();
        for (RsGenericParameter param : PsiElementUtil.stubChildrenOfType(list, RsGenericParameter.class)) {
            boolean include = false;
            if (param instanceof RsLifetimeParameter) {
                include = includeLifetimes;
            } else if (param instanceof RsTypeParameter) {
                include = includeTypes;
            } else if (param instanceof RsConstParameter) {
                include = includeConsts;
            }
            if (include) {
                result.add(param);
            }
        }
        return result;
    }
}
