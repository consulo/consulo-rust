/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.psi.ext;

import org.jetbrains.annotations.NotNull;
import org.rust.lang.core.psi.*;

import java.util.List;

/**
 * Delegates to {@link RsGenericDeclarationExtUtil} for the actual implementations.
 */
public final class RsGenericDeclarationExtUtil {

    private RsGenericDeclarationExtUtil() {
    }

    @NotNull
    public static List<RsTypeParameter> getTypeParameters(@NotNull RsGenericDeclaration decl) {
        return RsGenericDeclarationUtil.getTypeParameters(decl);
    }

    @NotNull
    public static List<RsLifetimeParameter> getLifetimeParameters(@NotNull RsGenericDeclaration decl) {
        return RsGenericDeclarationUtil.getLifetimeParameters(decl);
    }

    @NotNull
    public static List<RsConstParameter> getConstParameters(@NotNull RsGenericDeclaration decl) {
        return RsGenericDeclarationUtil.getConstParameters(decl);
    }

    @NotNull
    public static List<RsGenericParameter> getRequiredGenericParameters(@NotNull RsGenericDeclaration decl) {
        return RsGenericDeclarationUtil.getRequiredGenericParameters(decl);
    }

    @NotNull
    public static List<RsGenericParameter> getGenericParameters(@NotNull RsGenericDeclaration decl) {
        return RsGenericDeclarationUtil.getGenericParameters(decl);
    }

    @NotNull
    public static List<RsWherePred> getWherePreds(@NotNull RsGenericDeclaration decl) {
        return RsGenericDeclarationUtil.getWherePreds(decl);
    }
}
