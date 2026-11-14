/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.types.ty;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.rust.lang.core.psi.*;
import org.rust.lang.core.psi.ext.impl.RsGenericDeclarationUtil;
import org.rust.lang.core.psi.ext.RsStructOrEnumItemElement;
import org.rust.lang.core.types.*;
import org.rust.lang.core.types.consts.Const;
import org.rust.lang.core.types.infer.TypeFolder;
import org.rust.lang.core.types.infer.TypeVisitor;

import java.util.*;
import consulo.util.lang.Pair;
import org.rust.lang.core.psi.RsTypeParameter;
import org.rust.lang.core.psi.RsConstParameter;
import org.rust.lang.core.types.consts.CtUnknown;

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
    /** The type arguments in the order the item declares its type parameters - `Result<T, E>` is `[T, E]`. */
    public List<Ty> getTypeArguments() {
        List<RsTypeParameter> parameters =
            RsGenericDeclarationUtil.getTypeParameters(myItem);
        List<Ty> result = new ArrayList<>(parameters.size());
        for (RsTypeParameter parameter : parameters) {
            Ty ty = mySubst.get(parameter);
            result.add(ty != null ? ty : TyUnknown.INSTANCE);
        }
        return result;
    }

    /** The const arguments in declaration order. */
    @Nonnull
    public List<Const> getConstArguments() {
        List<RsConstParameter> parameters =
            RsGenericDeclarationUtil.getConstParameters(myItem);
        List<Const> result = new ArrayList<>(parameters.size());
        for (RsConstParameter parameter : parameters) {
            Const value = mySubst.get(parameter);
            result.add(value != null ? value : CtUnknown.INSTANCE);
        }
        return result;
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
