/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.types.infer;

import jakarta.annotation.Nonnull;
import org.rust.lang.core.types.ty.Ty;
import org.rust.lang.core.types.ty.TyUnknown;

public class ExpectedType implements TypeFoldable<ExpectedType> {
    public static final ExpectedType UNKNOWN = new ExpectedType(TyUnknown.INSTANCE, false);

    @Nonnull
    private final Ty myTy;
    private final boolean myCoercable;

    public ExpectedType(@Nonnull Ty ty) {
        this(ty, false);
    }

    public ExpectedType(@Nonnull Ty ty, boolean coercable) {
        myTy = ty;
        myCoercable = coercable;
    }

    @Nonnull
    public Ty getTy() {
        return myTy;
    }

    public boolean isCoercable() {
        return myCoercable;
    }

    @Nonnull
    public ExpectedType withCoercable(boolean coercable) {
        return new ExpectedType(myTy, coercable);
    }

    @Nonnull
    @Override
    public ExpectedType superFoldWith(@Nonnull TypeFolder folder) {
        return new ExpectedType(myTy.foldWith(folder), myCoercable);
    }

    @Override
    public boolean superVisitWith(@Nonnull TypeVisitor visitor) {
        return myTy.visitWith(visitor);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ExpectedType that = (ExpectedType) o;
        return myCoercable == that.myCoercable && myTy.equals(that.myTy);
    }

    @Override
    public int hashCode() {
        int result = myTy.hashCode();
        result = 31 * result + (myCoercable ? 1 : 0);
        return result;
    }
}
