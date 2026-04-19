/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.psi.ext;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.rust.lang.core.psi.ext.RsTraitOrImpl;

/**
 * Delegates to {@link RsAbstractableImplUtil} for the actual implementations.
 */
public final class RsAbstractableUtil {

    private RsAbstractableUtil() {
    }

    @NotNull
    public static RsAbstractableOwner getOwner(@NotNull RsAbstractable abstractable) {
        return RsAbstractableImplUtil.getOwner(abstractable);
    }

    @NotNull
    public static RsAbstractableOwner getOwnerBySyntaxOnly(@NotNull RsAbstractable abstractable) {
        return RsAbstractableImplUtil.getOwnerBySyntaxOnly(abstractable);
    }

    @Nullable
    public static RsAbstractable getSuperItem(@NotNull RsAbstractable abstractable) {
        return RsAbstractableImplUtil.getSuperItem(abstractable);
    }

    @Nullable
    public static RsAbstractable findCorrespondingElement(@NotNull RsTraitOrImpl traitOrImpl,
                                                           @NotNull RsAbstractable element) {
        return RsAbstractableImplUtil.findCorrespondingElement(traitOrImpl, element);
    }

    /**
     * Mirrors {@code fun RsAbstractable.searchForImplementations(): Query<RsAbstractable>} from
     * back to the same-named member (constant / function / type alias) of {@code abstractable}.
     */
    @NotNull
    public static java.util.List<RsAbstractable> searchForImplementations(@NotNull RsAbstractable abstractable) {
        org.rust.lang.core.psi.RsTraitItem traitItem =
            PsiElementUtil.ancestorStrict(abstractable, org.rust.lang.core.psi.RsTraitItem.class);
        if (traitItem == null) return java.util.Collections.emptyList();
        com.intellij.util.Query<org.rust.lang.core.psi.RsImplItem> traitImpls =
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
