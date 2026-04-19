/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.psi.ext;

import org.jetbrains.annotations.NotNull;
import org.rust.lang.core.psi.RsAssocTypeBinding;
import org.rust.lang.core.psi.RsLifetime;
import org.rust.lang.core.psi.RsTypeArgumentList;
import org.rust.lang.core.psi.RsTypeReference;

import java.util.Collections;
import java.util.List;

public final class RsMethodOrPathUtil {
    private RsMethodOrPathUtil() {
    }

    @NotNull
    public static List<RsLifetime> getLifetimeArguments(@NotNull RsMethodOrPath methodOrPath) {
        RsTypeArgumentList typeArgList = methodOrPath.getTypeArgumentList();
        return typeArgList != null ? RsTypeArgumentListUtil.getLifetimeArguments(typeArgList) : Collections.emptyList();
    }

    @NotNull
    public static List<RsTypeReference> getTypeArguments(@NotNull RsMethodOrPath methodOrPath) {
        RsTypeArgumentList typeArgList = methodOrPath.getTypeArgumentList();
        return typeArgList != null ? RsTypeArgumentListUtil.getTypeArguments(typeArgList) : Collections.emptyList();
    }

    @NotNull
    public static List<RsElement> getConstArguments(@NotNull RsMethodOrPath methodOrPath) {
        RsTypeArgumentList typeArgList = methodOrPath.getTypeArgumentList();
        return typeArgList != null ? RsTypeArgumentListUtil.getConstArguments(typeArgList) : Collections.emptyList();
    }

    @NotNull
    public static List<RsAssocTypeBinding> getAssocTypeBindings(@NotNull RsMethodOrPath methodOrPath) {
        RsTypeArgumentList typeArgList = methodOrPath.getTypeArgumentList();
        return typeArgList != null ? typeArgList.getAssocTypeBindingList() : Collections.emptyList();
    }

    @NotNull
    public static List<RsElement> getGenericArguments(
        @NotNull RsMethodOrPath methodOrPath,
        boolean includeLifetimes,
        boolean includeTypes,
        boolean includeConsts,
        boolean includeAssocBindings
    ) {
        RsTypeArgumentList typeArgList = methodOrPath.getTypeArgumentList();
        if (typeArgList == null) return Collections.emptyList();
        return RsTypeArgumentListUtil.getGenericArguments(typeArgList, includeLifetimes, includeTypes, includeConsts, includeAssocBindings);
    }

    @NotNull
    public static List<RsElement> getGenericArguments(@NotNull RsMethodOrPath methodOrPath) {
        return getGenericArguments(methodOrPath, true, true, true, true);
    }
}
