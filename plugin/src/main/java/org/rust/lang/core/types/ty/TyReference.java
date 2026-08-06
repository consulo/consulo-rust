/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.types.ty;

import jakarta.annotation.Nonnull;
import org.rust.lang.core.types.infer.TypeFolder;
import org.rust.lang.core.types.infer.TypeVisitor;
import org.rust.lang.core.types.regions.ReUnknown;
import org.rust.lang.core.types.regions.Region;

import java.util.Objects;

public class TyReference extends Ty {
    @Nonnull
    private final Ty myReferenced;
    @Nonnull
    private final Mutability myMutability;
    @Nonnull
    private final Region myRegion;

    public TyReference(@Nonnull Ty referenced, @Nonnull Mutability mutability) {
        this(referenced, mutability, ReUnknown.INSTANCE);
    }

    public TyReference(@Nonnull Ty referenced, @Nonnull Mutability mutability, @Nonnull Region region) {
        super(referenced.getFlags() | region.getFlags());
        myReferenced = referenced;
        myMutability = mutability;
        myRegion = region;
    }

    @Nonnull
    public Ty getReferenced() {
        return myReferenced;
    }

    @Nonnull
    public Mutability getMutability() {
        return myMutability;
    }

    @Nonnull
    public Region getRegion() {
        return myRegion;
    }

    @Nonnull
    public TyReference copy(@Nonnull Mutability mutability) {
        return new TyReference(myReferenced, mutability, myRegion);
    }

    @Override
    @Nonnull
    public Ty superFoldWith(@Nonnull TypeFolder folder) {
        return new TyReference(myReferenced.foldWith(folder), myMutability, myRegion.superFoldWith(folder));
    }

    @Override
    public boolean superVisitWith(@Nonnull TypeVisitor visitor) {
        return myReferenced.visitWith(visitor) || myRegion.superVisitWith(visitor);
    }

    @Override
    protected boolean isEquivalentToInner(@Nonnull Ty other) {
        if (!(other instanceof TyReference)) return false;
        TyReference otherRef = (TyReference) other;
        return myMutability == otherRef.myMutability
            && myReferenced.isEquivalentTo(otherRef.myReferenced);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TyReference that = (TyReference) o;
        return Objects.equals(myReferenced, that.myReferenced) && myMutability == that.myMutability;
    }

    @Override
    public int hashCode() {
        return Objects.hash(myReferenced, myMutability);
    }
}
