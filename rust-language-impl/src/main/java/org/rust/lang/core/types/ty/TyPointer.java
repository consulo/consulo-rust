/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.types.ty;

import jakarta.annotation.Nonnull;
import org.rust.lang.core.types.infer.TypeFolder;
import org.rust.lang.core.types.infer.TypeVisitor;

import java.util.Objects;

public class TyPointer extends Ty {
    @Nonnull
    private final Ty myReferenced;
    @Nonnull
    private final Mutability myMutability;

    public TyPointer(@Nonnull Ty referenced, @Nonnull Mutability mutability) {
        super(referenced.getFlags());
        myReferenced = referenced;
        myMutability = mutability;
    }

    @Nonnull
    public Ty getReferenced() {
        return myReferenced;
    }

    @Nonnull
    public Mutability getMutability() {
        return myMutability;
    }

    @Override
    @Nonnull
    public Ty superFoldWith(@Nonnull TypeFolder folder) {
        return new TyPointer(myReferenced.foldWith(folder), myMutability);
    }

    @Override
    public boolean superVisitWith(@Nonnull TypeVisitor visitor) {
        return myReferenced.visitWith(visitor);
    }

    @Override
    protected boolean isEquivalentToInner(@Nonnull Ty other) {
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
