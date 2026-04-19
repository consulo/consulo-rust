/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.types.ty;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.rust.lang.core.psi.*;
import org.rust.lang.core.psi.ext.RsGenericDeclarationUtil;
import org.rust.lang.core.psi.ext.RsStructOrEnumItemElement;
import org.rust.lang.core.types.*;
import org.rust.lang.core.types.consts.Const;
import org.rust.lang.core.types.infer.TypeFolder;
import org.rust.lang.core.types.infer.TypeVisitor;

import java.util.*;

public class TyAdt extends Ty {
    @NotNull
    private final RsStructOrEnumItemElement myItem;
    @NotNull
    private final Substitution mySubst;
    @Nullable
    private final BoundElement<RsTypeAlias> myAliasedBy;

    public TyAdt(@NotNull RsStructOrEnumItemElement item, @NotNull Substitution subst) {
        this(item, subst, null);
    }

    public TyAdt(@NotNull RsStructOrEnumItemElement item, @NotNull Substitution subst, @Nullable BoundElement<RsTypeAlias> aliasedBy) {
        super(KindUtil.mergeFlags(subst.getKinds()) | (aliasedBy != null ? KindUtil.mergeElementFlags(aliasedBy) : 0));
        myItem = item;
        mySubst = subst;
        myAliasedBy = aliasedBy;
    }

    @NotNull
    public RsStructOrEnumItemElement getItem() {
        return myItem;
    }

    @Override
    @NotNull
    public Substitution getTypeParameterValues() {
        return mySubst;
    }

    @Override
    @Nullable
    public BoundElement<RsTypeAlias> getAliasedBy() {
        return myAliasedBy;
    }

    @Override
    @NotNull
    public TyAdt withAlias(@NotNull BoundElement<RsTypeAlias> aliasedBy) {
        return new TyAdt(myItem, mySubst, aliasedBy);
    }

    @NotNull
    public List<Ty> getTypeArguments() {
        return new ArrayList<>(mySubst.getTypes());
    }

    @NotNull
    public List<Const> getConstArguments() {
        return new ArrayList<>(mySubst.getConsts());
    }

    @Override
    @NotNull
    public Ty superFoldWith(@NotNull TypeFolder folder) {
        BoundElement<RsTypeAlias> newAlias = myAliasedBy != null ? myAliasedBy.foldWith(folder) : null;
        return new TyAdt(myItem, mySubst.foldValues(folder), newAlias);
    }

    @Override
    public boolean superVisitWith(@NotNull TypeVisitor visitor) {
        return mySubst.visitValues(visitor);
    }

    @Override
    protected boolean isEquivalentToInner(@NotNull Ty other) {
        if (!(other instanceof TyAdt)) return false;
        TyAdt otherAdt = (TyAdt) other;
        if (!myItem.equals(otherAdt.myItem)) return false;
        for (com.intellij.openapi.util.Pair<Ty, Ty> pair : mySubst.zipTypeValues(otherAdt.mySubst)) {
            if (!pair.getFirst().isEquivalentTo(pair.getSecond())) return false;
        }
        return mySubst.getConstSubst().equals(otherAdt.mySubst.getConstSubst());
    }

    @NotNull
    public static TyAdt valueOf(@NotNull RsStructItem struct) {
        Substitution subst = RsGenericDeclarationUtil.withDefaultSubst(struct).getSubst();
        return new TyAdt(struct, subst);
    }

    @NotNull
    public static TyAdt valueOf(@NotNull RsEnumItem enumItem) {
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
