/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.psi.ext;

import org.rust.lang.core.psi.ext.PsiElementUtil;
import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;
import org.rust.lang.core.psi.*;
import org.rust.lang.core.resolve.ref.RsPathReference;

import java.util.ArrayList;
import java.util.List;

public final class RsTypeArgumentListUtil {
    private RsTypeArgumentListUtil() {
    }

    @NotNull
    public static List<RsElement> getGenericArguments(@NotNull RsTypeArgumentList list) {
        return getGenericArguments(list, true, true, true, true);
    }

    @NotNull
    public static List<RsElement> getGenericArguments(
        @NotNull RsTypeArgumentList list,
        boolean includeLifetimes,
        boolean includeTypes,
        boolean includeConsts,
        boolean includeAssocBindings
    ) {
        List<RsTypeReference> typeArguments = getTypeArguments(list);
        List<RsElement> result = new ArrayList<>();
        for (RsElement it : PsiElementUtil.stubChildrenOfType(list, RsElement.class)) {
            boolean include = false;
            if (it instanceof RsLifetime) {
                include = includeLifetimes;
            } else if (it instanceof RsTypeReference && typeArguments.contains(it)) {
                include = includeTypes;
            } else if (it instanceof RsExpr || (it instanceof RsTypeReference && !typeArguments.contains(it))) {
                include = includeConsts;
            } else if (it instanceof RsAssocTypeBinding) {
                include = includeAssocBindings;
            }
            if (include) {
                result.add(it);
            }
        }
        return result;
    }

    @NotNull
    public static List<RsLifetime> getLifetimeArguments(@NotNull RsTypeArgumentList list) {
        return list.getLifetimeList();
    }

    @NotNull
    public static List<RsTypeReference> getTypeArguments(@NotNull RsTypeArgumentList list) {
        List<RsTypeReference> result = new ArrayList<>();
        for (RsTypeReference ref : list.getTypeReferenceList()) {
            if (ref instanceof RsPathType) {
                RsPathType pathType = (RsPathType) ref;
                RsPathReference pathRef = pathType.getPath().getReference();
                if (pathRef != null) {
                    PsiElement resolved = pathRef.resolve();
                    if (resolved instanceof RsConstant || resolved instanceof RsFunction || resolved instanceof RsConstParameter) {
                        continue;
                    }
                }
            }
            result.add(ref);
        }
        return result;
    }

    @NotNull
    public static List<RsElement> getConstArguments(@NotNull RsTypeArgumentList list) {
        List<RsTypeReference> typeArguments = getTypeArguments(list);
        List<RsElement> result = new ArrayList<>();
        for (RsElement it : PsiElementUtil.stubChildrenOfType(list, RsElement.class)) {
            if (it instanceof RsExpr || (it instanceof RsTypeReference && !typeArguments.contains(it))) {
                result.add(it);
            }
        }
        return result;
    }
}
