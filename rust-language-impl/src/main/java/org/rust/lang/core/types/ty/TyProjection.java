/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.types.ty;

import jakarta.annotation.Nonnull;
import org.rust.lang.core.psi.RsTraitItem;
import org.rust.lang.core.psi.RsTypeAlias;
import org.rust.lang.core.types.*;
import org.rust.lang.core.types.infer.TypeFolder;
import org.rust.lang.core.types.infer.TypeVisitor;

import java.util.Objects;

public class TyProjection extends Ty {
    @Nonnull
    private final Ty myType;
    @Nonnull
    private final BoundElement<RsTraitItem> myTrait;
    @Nonnull
    private final BoundElement<RsTypeAlias> myTarget;

    public TyProjection(@Nonnull Ty type, @Nonnull BoundElement<RsTraitItem> trait, @Nonnull BoundElement<RsTypeAlias> target) {
        super(KindUtil.HAS_TY_PROJECTION_MASK | type.getFlags() | KindUtil.mergeElementFlags(trait) | KindUtil.mergeElementFlags(target));
        myType = type;
        myTrait = trait;
        myTarget = target;
    }

    @Nonnull
    public Ty getType() {
        return myType;
    }

    @Nonnull
    public BoundElement<RsTraitItem> getTrait() {
        return myTrait;
    }

    @Nonnull
    public BoundElement<RsTypeAlias> getTarget() {
        return myTarget;
    }

    @Nonnull
    public TraitRef getTraitRef() {
        return new TraitRef(myType, myTrait);
    }

    @Override
    @Nonnull
    public Ty superFoldWith(@Nonnull TypeFolder folder) {
        return new TyProjection(myType.foldWith(folder), myTrait.foldWith(folder), myTarget.foldWith(folder));
    }

    @Override
    public boolean superVisitWith(@Nonnull TypeVisitor visitor) {
        return myType.visitWith(visitor) || myTrait.visitWith(visitor) || myTarget.visitWith(visitor);
    }

    @Nonnull
    public static TyProjection valueOf(@Nonnull BoundElement<RsTypeAlias> boundAlias) {
        RsTypeAlias alias = boundAlias.element();
        RsTraitItem trait = (RsTraitItem) alias.getParent().getParent();
        Substitution subst = boundAlias.getSubst();
        Ty selfTy = subst.get(TyTypeParameter.self());
        if (selfTy == null) selfTy = TyTypeParameter.self(trait);
        BoundElement<RsTraitItem> boundTrait = new BoundElement<>(trait, subst);
        return new TyProjection(selfTy, boundTrait, boundAlias);
    }

    @Nonnull
    public static TyProjection valueOf(@Nonnull Ty selfTy, @Nonnull BoundElement<RsTypeAlias> boundAlias) {
        RsTypeAlias alias = boundAlias.element();
        RsTraitItem trait = (RsTraitItem) alias.getParent().getParent();
        Substitution subst = boundAlias.getSubst();
        BoundElement<RsTraitItem> boundTrait = new BoundElement<>(trait, subst);
        return new TyProjection(selfTy, boundTrait, boundAlias);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TyProjection that = (TyProjection) o;
        return Objects.equals(myType, that.myType) && Objects.equals(myTrait, that.myTrait) && Objects.equals(myTarget, that.myTarget);
    }

    @Override
    public int hashCode() {
        return Objects.hash(myType, myTrait, myTarget);
    }
}
