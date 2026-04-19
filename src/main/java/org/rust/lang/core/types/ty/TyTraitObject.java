/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.types.ty;

import org.jetbrains.annotations.NotNull;
import org.rust.lang.core.psi.RsTraitItem;
import org.rust.lang.core.psi.ext.RsGenericDeclarationUtil;
import org.rust.lang.core.types.*;
import org.rust.lang.core.types.infer.TypeFolder;
import org.rust.lang.core.types.infer.TypeVisitor;
import org.rust.lang.core.types.regions.Region;
import org.rust.lang.core.types.regions.ReStatic;

import java.util.*;

public class TyTraitObject extends Ty {
    @NotNull
    private final List<BoundElement<RsTraitItem>> myTraits;
    @NotNull
    private final Region myRegion;
    private final boolean myHasUnresolvedBound;

    public TyTraitObject(@NotNull List<BoundElement<RsTraitItem>> traits, @NotNull Region region) {
        this(traits, region, false);
    }

    public TyTraitObject(@NotNull List<BoundElement<RsTraitItem>> traits, @NotNull Region region, boolean hasUnresolvedBound) {
        super(KindUtil.mergeElementFlags(new ArrayList<>(traits)) | region.getFlags());
        myTraits = traits;
        myRegion = region;
        myHasUnresolvedBound = hasUnresolvedBound;
    }

    @NotNull
    public List<BoundElement<RsTraitItem>> getTraits() {
        return myTraits;
    }

    @NotNull
    public Region getRegion() {
        return myRegion;
    }

    public boolean getHasUnresolvedBound() {
        return myHasUnresolvedBound;
    }

    @Override
    @NotNull
    public Ty superFoldWith(@NotNull TypeFolder folder) {
        List<BoundElement<RsTraitItem>> newTraits = new ArrayList<>();
        for (BoundElement<RsTraitItem> trait : myTraits) {
            newTraits.add(trait.foldWith(folder));
        }
        return new TyTraitObject(newTraits, myRegion.superFoldWith(folder), myHasUnresolvedBound);
    }

    @Override
    public boolean superVisitWith(@NotNull TypeVisitor visitor) {
        for (BoundElement<RsTraitItem> trait : myTraits) {
            if (trait.visitWith(visitor)) return true;
        }
        return myRegion.superVisitWith(visitor);
    }

    @NotNull
    public static TyTraitObject valueOf(@NotNull RsTraitItem trait) {
        BoundElement<?> be = RsGenericDeclarationUtil.withDefaultSubst(trait);
        @SuppressWarnings("unchecked")
        BoundElement<RsTraitItem> bound = new BoundElement<>(trait, be.getSubst(), be.getAssoc());
        return new TyTraitObject(Collections.singletonList(bound), ReStatic.INSTANCE);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TyTraitObject that = (TyTraitObject) o;
        return Objects.equals(myTraits, that.myTraits);
    }

    @Override
    public int hashCode() {
        return Objects.hash(myTraits);
    }
}
