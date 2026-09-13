/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.psi.ext.impl;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.rust.lang.core.psi.ext.RsTraitOrImpl;
import consulo.application.util.query.Query;
import org.rust.lang.core.psi.RsConstant;
import org.rust.lang.core.psi.RsFunction;
import org.rust.lang.core.psi.RsImplItem;
import org.rust.lang.core.psi.RsTraitItem;
import org.rust.lang.core.psi.RsTypeAlias;
import org.rust.lang.core.psi.ext.*;

/**
 * Delegates to {@link RsAbstractableImplUtil} for the actual implementations.
 */
public final class RsAbstractableUtil {

    private RsAbstractableUtil() {
    }

    @Nonnull
    public static RsAbstractableOwner getOwner(@Nonnull RsAbstractable abstractable) {
        return RsAbstractableImplUtil.getOwner(abstractable);
    }

    @Nonnull
    public static RsAbstractableOwner getOwnerBySyntaxOnly(@Nonnull RsAbstractable abstractable) {
        return RsAbstractableImplUtil.getOwnerBySyntaxOnly(abstractable);
    }

    @Nullable
    public static RsAbstractable getSuperItem(@Nonnull RsAbstractable abstractable) {
        return RsAbstractableImplUtil.getSuperItem(abstractable);
    }

    @Nullable
    public static RsAbstractable findCorrespondingElement(@Nonnull RsTraitOrImpl traitOrImpl,
                                                           @Nonnull RsAbstractable element) {
        return RsAbstractableImplUtil.findCorrespondingElement(traitOrImpl, element);
    }

    /**
     * Mirrors {@code fun RsAbstractable.searchForImplementations(): Query<RsAbstractable>} from
     * back to the same-named member (constant / function / type alias) of {@code abstractable}.
     */
    @Nonnull
    public static java.util.List<RsAbstractable> searchForImplementations(@Nonnull RsAbstractable abstractable) {
        org.rust.lang.core.psi.RsTraitItem traitItem =
            PsiElementUtil.ancestorStrict(abstractable, org.rust.lang.core.psi.RsTraitItem.class);
        if (traitItem == null) return java.util.Collections.emptyList();
        consulo.application.util.query.Query<org.rust.lang.core.psi.RsImplItem> traitImpls =
            RsTraitItemUtil.searchForImplementations(traitItem);
        if (traitImpls == null) return java.util.Collections.emptyList();

        String name = abstractable instanceof org.rust.lang.core.psi.ext.RsNamedElement
            ? ((org.rust.lang.core.psi.ext.RsNamedElement) abstractable).getName()
            : null;
        if (name == null) return java.util.Collections.emptyList();

        java.util.List<RsAbstractable> result = new java.util.ArrayList<>();
        for (org.rust.lang.core.psi.RsImplItem impl : traitImpls.findAll()) {
            java.util.List<RsAbstractable> members = RsMembersUtil.getExpandedMembers(impl);
            for (RsAbstractable m : members) {
                if (!name.equals(((org.rust.lang.core.psi.ext.RsNamedElement) m).getName())) continue;
                if (abstractable instanceof org.rust.lang.core.psi.RsConstant
                    && m instanceof org.rust.lang.core.psi.RsConstant) {
                    result.add(m);
                } else if (abstractable instanceof org.rust.lang.core.psi.RsFunction
                    && m instanceof org.rust.lang.core.psi.RsFunction) {
                    result.add(m);
                } else if (abstractable instanceof org.rust.lang.core.psi.RsTypeAlias
                    && m instanceof org.rust.lang.core.psi.RsTypeAlias) {
                    result.add(m);
                }
            }
        }
        return result;
    }
}
