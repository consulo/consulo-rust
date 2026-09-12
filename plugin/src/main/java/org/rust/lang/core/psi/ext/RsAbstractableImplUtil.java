/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.psi.ext;

import consulo.language.psi.PsiElement;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.rust.lang.core.psi.RsConstant;
import org.rust.lang.core.psi.RsForeignModItem;
import org.rust.lang.core.psi.RsFunction;
import org.rust.lang.core.psi.RsImplItem;
import org.rust.lang.core.psi.RsMembers;
import org.rust.lang.core.psi.RsTraitItem;
import org.rust.lang.core.psi.RsTraitRef;
import org.rust.lang.core.psi.RsTypeAlias;

import java.util.List;
import java.util.function.Function;

public final class RsAbstractableImplUtil {

    private RsAbstractableImplUtil() {
    }

    /**
     * The owner as seen through the context chain, so macro-expanded elements report the
     * trait or impl they were expanded into.
     */
    @Nonnull
    public static RsAbstractableOwner getOwner(@Nonnull RsAbstractable abstractable) {
        return ownerBy(abstractable, PsiElement::getContext);
    }

    /**
     * The owner as seen through the stub tree only, ignoring macro expansion context.
     */
    @Nonnull
    public static RsAbstractableOwner getOwnerBySyntaxOnly(@Nonnull RsAbstractable abstractable) {
        return ownerBy(abstractable, RsElementUtil::getStubParent);
    }

    @Nonnull
    private static RsAbstractableOwner ownerBy(@Nonnull RsAbstractable abstractable,
                                               @Nonnull Function<PsiElement, PsiElement> getAncestor) {
        PsiElement ancestor = getAncestor.apply(abstractable);
        if (ancestor instanceof RsForeignModItem) {
            return RsAbstractableOwner.Foreign;
        }
        if (ancestor instanceof RsMembers) {
            PsiElement traitOrImpl = getAncestor.apply(ancestor);
            if (traitOrImpl instanceof RsImplItem) {
                RsImplItem impl = (RsImplItem) traitOrImpl;
                return new RsAbstractableOwner.Impl(impl, impl.getTraitRef() == null);
            }
            if (traitOrImpl instanceof RsTraitItem) {
                return new RsAbstractableOwner.Trait((RsTraitItem) traitOrImpl);
            }
        }
        return RsAbstractableOwner.Free;
    }

    /**
     * For a member of a trait impl, the corresponding member declared in the trait itself.
     */
    @Nullable
    public static RsAbstractable getSuperItem(@Nonnull RsAbstractable abstractable) {
        RsAbstractableOwner owner = getOwner(abstractable);
        if (!(owner instanceof RsAbstractableOwner.Impl)) return null;

        RsTraitRef traitRef = ((RsAbstractableOwner.Impl) owner).getImpl().getTraitRef();
        if (traitRef == null) return null;

        RsTraitItem superTrait = RsTraitRefUtil.resolveToTrait(traitRef);
        if (superTrait == null) return null;

        return findCorrespondingElement(superTrait, abstractable);
    }

    /**
     * The member of {@code traitOrImpl} matching {@code element} by kind and name.
     */
    @Nullable
    public static RsAbstractable findCorrespondingElement(@Nonnull RsTraitOrImpl traitOrImpl,
                                                          @Nonnull RsAbstractable element) {
        String name = element instanceof RsNamedElement ? ((RsNamedElement) element).getName() : null;
        if (name == null) return null;

        List<RsAbstractable> members = RsMembersUtil.getExpandedMembers(traitOrImpl);

        if (element instanceof RsConstant) {
            for (RsConstant candidate : RsMembersUtil.getConstants(members)) {
                if (name.equals(candidate.getName())) return candidate;
            }
        }
        else if (element instanceof RsFunction) {
            for (RsFunction candidate : RsMembersUtil.getFunctions(members)) {
                if (name.equals(candidate.getName())) return candidate;
            }
        }
        else if (element instanceof RsTypeAlias) {
            for (RsTypeAlias candidate : RsMembersUtil.getTypes(members)) {
                if (name.equals(candidate.getName())) return candidate;
            }
        }
        return null;
    }
}
