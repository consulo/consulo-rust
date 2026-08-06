/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.types.ty;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.rust.lang.core.psi.*;
import org.rust.lang.core.psi.ext.RsGenericDeclarationUtil;
import org.rust.lang.core.psi.ext.RsStructOrEnumItemElement;
import org.rust.lang.core.types.*;
import org.rust.lang.core.types.consts.Const;
import org.rust.lang.core.types.infer.TypeFolder;
import org.rust.lang.core.types.infer.TypeVisitor;

import java.util.*;

public class TyAdt extends Ty {
    @Nonnull
    private final RsStructOrEnumItemElement myItem;
    @Nonnull
    private final Substitution mySubst;
    @Nullable
    private final BoundElement<RsTypeAlias> myAliasedBy;

    public TyAdt(@Nonnull RsStructOrEnumItemElement item, @Nonnull Substitution subst) {
        this(item, subst, null);
    }

    public TyAdt(@Nonnull RsStructOrEnumItemElement item, @Nonnull Substitution subst, @Nullable BoundElement<RsTypeAlias> aliasedBy) {
        super(KindUtil.mergeFlags(subst.getKinds()) | (aliasedBy != null ? KindUtil.mergeElementFlags(aliasedBy) : 0));
        myItem = item;
        mySubst = subst;
        myAliasedBy = aliasedBy;
    }

    @Nonnull
    public RsStructOrEnumItemElement getItem() {
        return myItem;
    }

    @Override
    @Nonnull
    public Substitution getTypeParameterValues() {
        return mySubst;
    }

    @Override
    @Nullable
    public BoundElement<RsTypeAlias> getAliasedBy() {
        return myAliasedBy;
    }

    @Override
    @Nonnull
    public TyAdt withAlias(@Nonnull BoundElement<RsTypeAlias> aliasedBy) {
        return new TyAdt(myItem, mySubst, aliasedBy);
    }

    @Nonnull
    public List<Ty> getTypeArguments() {
        return new ArrayList<>(mySubst.getTypes());
    }

    @Nonnull
    public List<Const> getConstArguments() {
        return new ArrayList<>(mySubst.getConsts());
    }

    @Override
    @Nonnull
    public Ty superFoldWith(@Nonnull TypeFolder folder) {
        BoundElement<RsTypeAlias> newAlias = myAliasedBy != null ? myAliasedBy.foldWith(folder) : null;
        return new TyAdt(myItem, mySubst.foldValues(folder), newAlias);
    }

    @Override
    public boolean superVisitWith(@Nonnull TypeVisitor visitor) {
        return mySubst.visitValues(visitor);
    }

    @Override
    protected boolean isEquivalentToInner(@Nonnull Ty other) {
        if (!(other instanceof TyAdt)) return false;
        TyAdt otherAdt = (TyAdt) other;
        if (!myItem.equals(otherAdt.myItem)) return false;
        for (consulo.util.lang.Pair<Ty, Ty> pair : mySubst.zipTypeValues(otherAdt.mySubst)) {
            if (!pair.getFirst().isEquivalentTo(pair.getSecond())) return false;
        }
        return mySubst.getConstSubst().equals(otherAdt.mySubst.getConstSubst());
    }

    @Nonnull
    public static TyAdt valueOf(@Nonnull RsStructItem struct) {
        Substitution subst = RsGenericDeclarationUtil.withDefaultSubst(struct).getSubst();
        return new TyAdt(struct, subst);
    }

    @Nonnull
    public static TyAdt valueOf(@Nonnull RsEnumItem enumItem) {
        Substitution subst = RsGenericDeclarationUtil.withDefaultSubst(enumItem).getSubst();
        return new TyAdt(enumItem, subst);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TyAdt tyAdt = (TyAdt) o;
        return Objects.equals(myItem, tyAdt.myItem)
            && Objects.equals(mySubst, tyAdt.mySubst);
    }

    @Override
    public int hashCode() {
        return Objects.hash(myItem, mySubst);
    }
}
