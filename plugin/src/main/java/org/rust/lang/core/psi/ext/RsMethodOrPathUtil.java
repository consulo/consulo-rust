/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.psi.ext;

import jakarta.annotation.Nonnull;
import org.rust.lang.core.psi.RsAssocTypeBinding;
import org.rust.lang.core.psi.RsLifetime;
import org.rust.lang.core.psi.RsTypeArgumentList;
import org.rust.lang.core.psi.RsTypeReference;

import java.util.Collections;
import java.util.List;

public final class RsMethodOrPathUtil {
    private RsMethodOrPathUtil() {
    }

    @Nonnull
    public static List<RsLifetime> getLifetimeArguments(@Nonnull RsMethodOrPath methodOrPath) {
        RsTypeArgumentList typeArgList = methodOrPath.getTypeArgumentList();
        return typeArgList != null ? RsTypeArgumentListUtil.getLifetimeArguments(typeArgList) : Collections.emptyList();
    }

    @Nonnull
    public static List<RsTypeReference> getTypeArguments(@Nonnull RsMethodOrPath methodOrPath) {
        RsTypeArgumentList typeArgList = methodOrPath.getTypeArgumentList();
        return typeArgList != null ? RsTypeArgumentListUtil.getTypeArguments(typeArgList) : Collections.emptyList();
    }

    @Nonnull
    public static List<RsElement> getConstArguments(@Nonnull RsMethodOrPath methodOrPath) {
        RsTypeArgumentList typeArgList = methodOrPath.getTypeArgumentList();
        return typeArgList != null ? RsTypeArgumentListUtil.getConstArguments(typeArgList) : Collections.emptyList();
    }

    @Nonnull
    public static List<RsAssocTypeBinding> getAssocTypeBindings(@Nonnull RsMethodOrPath methodOrPath) {
        RsTypeArgumentList typeArgList = methodOrPath.getTypeArgumentList();
        return typeArgList != null ? typeArgList.getAssocTypeBindingList() : Collections.emptyList();
    }

    @Nonnull
    public static List<RsElement> getGenericArguments(
        @Nonnull RsMethodOrPath methodOrPath,
        boolean includeLifetimes,
        boolean includeTypes,
        boolean includeConsts,
        boolean includeAssocBindings
    ) {
        RsTypeArgumentList typeArgList = methodOrPath.getTypeArgumentList();
        if (typeArgList == null) return Collections.emptyList();
        return RsTypeArgumentListUtil.getGenericArguments(typeArgList, includeLifetimes, includeTypes, includeConsts, includeAssocBindings);
    }

    @Nonnull
    public static List<RsElement> getGenericArguments(@Nonnull RsMethodOrPath methodOrPath) {
        return getGenericArguments(methodOrPath, true, true, true, true);
    }
}
