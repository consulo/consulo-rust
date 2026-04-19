/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.types.ty;

import org.jetbrains.annotations.NotNull;
import org.rust.lang.core.types.infer.TypeFolder;
import org.rust.lang.core.types.infer.TypeVisitor;
import org.rust.lang.core.types.regions.ReUnknown;
import org.rust.lang.core.types.regions.Region;

import java.util.Objects;

public class TyReference extends Ty {
    @NotNull
    private final Ty myReferenced;
    @NotNull
    private final Mutability myMutability;
    @NotNull
    private final Region myRegion;

    public TyReference(@NotNull Ty referenced, @NotNull Mutability mutability) {
        this(referenced, mutability, ReUnknown.INSTANCE);
    }

    public TyReference(@NotNull Ty referenced, @NotNull Mutability mutability, @NotNull Region region) {
        super(referenced.getFlags() | region.getFlags());
        myReferenced = referenced;
        myMutability = mutability;
        myRegion = region;
    }

    @NotNull
    public Ty getReferenced() {
        return myReferenced;
    }

    @NotNull
    public Mutability getMutability() {
        return myMutability;
    }

    @NotNull
    public Region getRegion() {
        return myRegion;
    }

    @NotNull
    public TyReference copy(@NotNull Mutability mutability) {
        return new TyReference(myReferenced, mutability, myRegion);
    }

    @Override
    @NotNull
    public Ty superFoldWith(@NotNull TypeFolder folder) {
        return new TyReference(myReferenced.foldWith(folder), myMutability, myRegion.superFoldWith(folder));
    }

    @Override
    public boolean superVisitWith(@NotNull TypeVisitor visitor) {
        return myReferenced.visitWith(visitor) || myRegion.superVisitWith(visitor);
    }

    @Override
    protected boolean isEquivalentToInner(@NotNull Ty other) {
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
