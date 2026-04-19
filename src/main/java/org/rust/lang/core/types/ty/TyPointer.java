/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.types.ty;

import org.jetbrains.annotations.NotNull;
import org.rust.lang.core.types.infer.TypeFolder;
import org.rust.lang.core.types.infer.TypeVisitor;

import java.util.Objects;

public class TyPointer extends Ty {
    @NotNull
    private final Ty myReferenced;
    @NotNull
    private final Mutability myMutability;

    public TyPointer(@NotNull Ty referenced, @NotNull Mutability mutability) {
        super(referenced.getFlags());
        myReferenced = referenced;
        myMutability = mutability;
    }

    @NotNull
    public Ty getReferenced() {
        return myReferenced;
    }

    @NotNull
    public Mutability getMutability() {
        return myMutability;
    }

    @Override
    @NotNull
    public Ty superFoldWith(@NotNull TypeFolder folder) {
        return new TyPointer(myReferenced.foldWith(folder), myMutability);
    }

    @Override
    public boolean superVisitWith(@NotNull TypeVisitor visitor) {
        return myReferenced.visitWith(visitor);
    }

    @Override
    protected boolean isEquivalentToInner(@NotNull Ty other) {
        if (!(other instanceof TyPointer)) return false;
        TyPointer otherPtr = (TyPointer) other;
        return myMutability == otherPtr.myMutability
            && myReferenced.isEquivalentTo(otherPtr.myReferenced);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TyPointer that = (TyPointer) o;
        return Objects.equals(myReferenced, that.myReferenced) && myMutability == that.myMutability;
    }

    @Override
    public int hashCode() {
        return Objects.hash(myReferenced, myMutability);
    }
}
