/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.types;

import jakarta.annotation.Nonnull;
import org.rust.lang.core.psi.*;
import org.rust.lang.core.psi.ext.RsAbstractableOwner;
import org.rust.lang.core.psi.ext.RsAbstractableOwnerUtil;
import org.rust.lang.core.psi.ext.RsGenericDeclarationUtil;
import org.rust.lang.core.types.infer.TyLowering;
import org.rust.lang.core.types.ty.*;
import org.rust.openapiext.OpenApiUtil;

public final class RsPsiTypeImplUtil {
    public static final RsPsiTypeImplUtil INSTANCE = new RsPsiTypeImplUtil();

    private RsPsiTypeImplUtil() {
    }

    @Nonnull
    public static Ty declaredType(@Nonnull RsStructItem psi) {
        return TyAdt.valueOf(psi);
    }

    @Nonnull
    public static Ty declaredType(@Nonnull RsEnumItem psi) {
        return TyAdt.valueOf(psi);
    }

    @Nonnull
    public static Ty declaredType(@Nonnull RsTraitItem psi) {
        return TyTraitObject.valueOf(psi);
    }

    @Nonnull
    public static Ty declaredType(@Nonnull RsTypeParameter psi) {
        return TyTypeParameter.named(psi);
    }

    @Nonnull
    public static Ty declaredType(@Nonnull RsImplItem psi) {
        return TyTypeParameter.self(psi);
    }

    @Nonnull
    public static Ty declaredType(@Nonnull RsTypeAlias psi) {
        RsTypeReference typeReference = psi.getTypeReference();
        if (typeReference != null) {
            Ty ty = OpenApiUtil.recursionGuard(typeReference, () -> TyLowering.lowerTypeReference(typeReference));
            return ty != null ? ty : TyUnknown.INSTANCE;
        }
        RsAbstractableOwner owner = RsAbstractableOwnerUtil.getOwner(psi);
        if (owner == RsAbstractableOwner.Free) return TyUnknown.INSTANCE;
        if (owner instanceof RsAbstractableOwner.Trait) {
            return TyProjection.valueOf(RsGenericDeclarationUtil.withDefaultSubst(psi));
        }
        if (owner instanceof RsAbstractableOwner.Impl) return TyUnknown.INSTANCE;
        if (owner == RsAbstractableOwner.Foreign) return TyUnknown.INSTANCE;
        return TyUnknown.INSTANCE;
    }
}
