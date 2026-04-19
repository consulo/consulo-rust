/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.types.infer;

import org.jetbrains.annotations.NotNull;
import org.rust.lang.core.types.ty.Ty;
import org.rust.lang.core.types.ty.TyUnknown;

public class ExpectedType implements TypeFoldable<ExpectedType> {
    public static final ExpectedType UNKNOWN = new ExpectedType(TyUnknown.INSTANCE, false);

    @NotNull
    private final Ty myTy;
    private final boolean myCoercable;

    public ExpectedType(@NotNull Ty ty) {
        this(ty, false);
    }

    public ExpectedType(@NotNull Ty ty, boolean coercable) {
        myTy = ty;
        myCoercable = coercable;
    }

    @NotNull
    public Ty getTy() {
        return myTy;
    }

    public boolean isCoercable() {
        return myCoercable;
    }

    @NotNull
    public ExpectedType withCoercable(boolean coercable) {
        return new ExpectedType(myTy, coercable);
    }

    @NotNull
    @Override
    public ExpectedType superFoldWith(@NotNull TypeFolder folder) {
        return new ExpectedType(myTy.foldWith(folder), myCoercable);
    }

    @Override
    public boolean superVisitWith(@NotNull TypeVisitor visitor) {
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
