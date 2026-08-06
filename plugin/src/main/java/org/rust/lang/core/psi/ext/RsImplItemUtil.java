/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.psi.ext;

import consulo.language.psi.PsiElement;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.rust.lang.core.psi.*;
import org.rust.lang.core.stubs.RsImplItemStub;
import org.rust.lang.core.types.NormTypeUtil;
import org.rust.lang.core.types.ty.Ty;
import org.rust.lang.core.types.ty.TyAdt;
import org.rust.lang.core.psi.ext.RsTraitRefUtil;

public final class RsImplItemUtil {
    private RsImplItemUtil() {
    }

    @Nullable
    public static PsiElement getDefault(@Nonnull RsImplItem impl) {
        consulo.language.ast.ASTNode node = impl.getNode().findChildByType(RsElementTypes.DEFAULT);
        return node != null ? node.getPsi() : null;
    }

    /** {@code impl !Sync for Bar} vs {@code impl Foo for Bar} */
    public static boolean isNegativeImpl(@Nonnull RsImplItem impl) {
        RsImplItemStub stub = RsPsiJavaUtil.getGreenStub(impl);
        if (stub != null) return stub.isNegativeImpl();
        return impl.getNode().findChildByType(RsElementTypes.EXCL) != null;
    }

    public static boolean isReservationImpl(@Nonnull RsImplItem impl) {
        return IMPL_ITEM_IS_RESERVATION_IMPL_PROP.getByPsi(impl);
    }

    @Nonnull
    public static final StubbedAttributeProperty<RsImplItem, RsImplItemStub> IMPL_ITEM_IS_RESERVATION_IMPL_PROP =
        new StubbedAttributeProperty<>(
            attrs -> attrs.hasAttribute("rustc_reservation_impl"),
            RsImplItemStub::getMayBeReservationImpl
        );

    @Nullable
    public static TyAdt getImplementingType(@Nonnull RsImplItem impl) {
        if (impl.getTypeReference() == null) return null;
        Ty ty = NormTypeUtil.getNormType(impl.getTypeReference());
        return ty instanceof TyAdt ? (TyAdt) ty : null;
    }

    @Nullable
    public static org.rust.lang.core.types.BoundElement<RsTraitItem> getImplementedTrait(@Nonnull RsImplItem impl) {
        return impl.getImplementedTrait();
    }

    /**
     * Checks orphan rules for impl items.
     * See <a href="https://doc.rust-lang.org/reference/items/implementations.html#orphan-rules">Orphan Rules</a>.
     */
    public static boolean checkOrphanRules(@Nonnull RsImplItem impl,
                                           @Nonnull java.util.function.Predicate<RsElement> isSameCrate) {
        RsTraitRef traitRef = impl.getTraitRef();
        if (traitRef == null) return true;
        org.rust.lang.core.types.BoundElement<RsTraitItem> bound = RsTraitRefUtil.resolveToBoundTrait(traitRef);
        if (bound == null) return true;
        if (isSameCrate.test(bound.element())) return true;
        // Simplified orphan check
        if (impl.getTypeReference() == null) return true;
        Ty implTy = NormTypeUtil.getNormType(impl.getTypeReference());
        if (implTy instanceof TyAdt && isSameCrate.test(((TyAdt) implTy).getItem())) return true;
        return false;
    }
}
