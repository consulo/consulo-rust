/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.types.ty;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.rust.lang.core.types.consts.Const;
import org.rust.lang.core.types.consts.CtValue;
import org.rust.lang.core.types.infer.TypeFolder;
import org.rust.lang.core.types.infer.TypeVisitor;

import java.util.Objects;

public class TyArray extends Ty {
    @Nonnull
    private final Ty myBase;
    @Nonnull
    private final Const myConst;

    public TyArray(@Nonnull Ty base, @Nonnull Const aConst) {
        super(base.getFlags() | aConst.getFlags());
        myBase = base;
        myConst = aConst;
    }

    @Nonnull
    public Ty getBase() {
        return myBase;
    }

    @Nonnull
    public Const getConst() {
        return myConst;
    }

    @Nullable
    public Long getSize() {
        return CtValue.asLong(myConst);
    }

    @Nonnull
    public TyArray copy(@Nonnull Const newConst) {
        return new TyArray(myBase, newConst);
    }

    @Override
    @Nonnull
    public Ty superFoldWith(@Nonnull TypeFolder folder) {
        return new TyArray(myBase.foldWith(folder), myConst.foldWith(folder));
    }

    @Override
    public boolean superVisitWith(@Nonnull TypeVisitor visitor) {
        return myBase.visitWith(visitor) || myConst.visitWith(visitor);
    }

    @Override
    protected boolean isEquivalentToInner(@Nonnull Ty other) {
        if (!(other instanceof TyArray)) return false;
        TyArray otherArray = (TyArray) other;
        return myBase.isEquivalentTo(otherArray.myBase)
            && Objects.equals(myConst, otherArray.myConst);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TyArray tyArray = (TyArray) o;
        return Objects.equals(myBase, tyArray.myBase) && Objects.equals(myConst, tyArray.myConst);
    }

    @Override
    public int hashCode() {
        return Objects.hash(myBase, myConst);
    }
}
