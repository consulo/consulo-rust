/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.psi.ext.impl;

import jakarta.annotation.Nonnull;
import org.rust.lang.core.psi.*;

import java.util.List;
import org.rust.lang.core.psi.ext.*;

/**
 * Delegates to {@link RsGenericDeclarationExtUtil} for the actual implementations.
 */
public final class RsGenericDeclarationExtUtil {

    private RsGenericDeclarationExtUtil() {
    }

    @Nonnull
    public static List<RsTypeParameter> getTypeParameters(@Nonnull RsGenericDeclaration decl) {
        return RsGenericDeclarationUtil.getTypeParameters(decl);
    }

    @Nonnull
    public static List<RsLifetimeParameter> getLifetimeParameters(@Nonnull RsGenericDeclaration decl) {
        return RsGenericDeclarationUtil.getLifetimeParameters(decl);
    }

    @Nonnull
    public static List<RsConstParameter> getConstParameters(@Nonnull RsGenericDeclaration decl) {
        return RsGenericDeclarationUtil.getConstParameters(decl);
    }

    @Nonnull
    public static List<RsGenericParameter> getRequiredGenericParameters(@Nonnull RsGenericDeclaration decl) {
        return RsGenericDeclarationUtil.getRequiredGenericParameters(decl);
    }

    @Nonnull
    public static List<RsGenericParameter> getGenericParameters(@Nonnull RsGenericDeclaration decl) {
        return RsGenericDeclarationUtil.getGenericParameters(decl);
    }

    @Nonnull
    public static List<RsWherePred> getWherePreds(@Nonnull RsGenericDeclaration decl) {
        return RsGenericDeclarationUtil.getWherePreds(decl);
    }
}
