/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.types;

import org.jetbrains.annotations.NotNull;
import org.rust.lang.core.psi.RsTraitItem;
import org.rust.lang.core.psi.ext.RsGenericDeclarationUtil;
import org.rust.lang.core.psi.ext.RsTraitItemUtil;
import org.rust.lang.core.types.infer.TypeFoldable;
import org.rust.lang.core.types.infer.TypeFolder;
import org.rust.lang.core.types.infer.TypeVisitor;
import org.rust.lang.core.types.ty.Ty;
import org.rust.lang.core.types.ty.TyTypeParameter;
import org.rust.lang.core.types.ty.TyUnknown;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * A complete reference to a trait. These take numerous guises in syntax,
 * but perhaps the most recognizable form is in a where clause:
 *     `T : Foo<U>`
 */
public class TraitRef implements TypeFoldable<TraitRef> {
    @NotNull
    private final Ty mySelfTy;
    @NotNull
    private final BoundElement<RsTraitItem> myTrait;

    public TraitRef(@NotNull Ty selfTy, @NotNull BoundElement<RsTraitItem> trait) {
        mySelfTy = selfTy;
        myTrait = trait;
    }

    @NotNull
    public Ty getSelfTy() {
        return mySelfTy;
    }

    @NotNull
    public BoundElement<RsTraitItem> getTrait() {
        return myTrait;
    }

    @NotNull
    public List<TraitRef> getFlattenHierarchy() {
        List<BoundElement<RsTraitItem>> flatBounds = new ArrayList<>(RsTraitItemUtil.getFlattenHierarchy(myTrait, mySelfTy));
        List<TraitRef> result = new ArrayList<>();
        for (BoundElement<RsTraitItem> bound : flatBounds) {
            result.add(new TraitRef(mySelfTy, bound));
        }
        return result;
    }

    @Override
    @NotNull
    public TraitRef superFoldWith(@NotNull TypeFolder folder) {
        return new TraitRef(mySelfTy.foldWith(folder), myTrait.foldWith(folder));
    }

    @Override
    public boolean superVisitWith(@NotNull TypeVisitor visitor) {
        return mySelfTy.visitWith(visitor) || myTrait.visitWith(visitor);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TraitRef traitRef = (TraitRef) o;
        return Objects.equals(mySelfTy, traitRef.mySelfTy) && Objects.equals(myTrait, traitRef.myTrait);
    }

    @Override
    public int hashCode() {
        return Objects.hash(mySelfTy, myTrait);
    }

    @Override
    public String toString() {
        RsTraitItem item = myTrait.element();
        Substitution subst = myTrait.getSubst();
        List<Ty> tyArgs = new ArrayList<>();
        for (org.rust.lang.core.psi.RsTypeParameter param : item.getTypeParameters()) {
            Ty ty = subst.get(param);
            tyArgs.add(ty != null ? ty : TyUnknown.INSTANCE);
        }
        String name = item.getName();
        if (tyArgs.isEmpty()) {
            return mySelfTy + ": " + name;
        } else {
            StringBuilder sb = new StringBuilder();
            sb.append(mySelfTy).append(": ").append(name).append("<");
            for (int i = 0; i < tyArgs.size(); i++) {
                if (i > 0) sb.append(", ");
                sb.append(tyArgs.get(i));
            }
            sb.append(">");
            return sb.toString();
        }
    }
}
