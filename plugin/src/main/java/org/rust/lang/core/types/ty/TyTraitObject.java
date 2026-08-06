/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.types.ty;

import jakarta.annotation.Nonnull;
import org.rust.lang.core.psi.RsTraitItem;
import org.rust.lang.core.psi.ext.RsGenericDeclarationUtil;
import org.rust.lang.core.types.*;
import org.rust.lang.core.types.infer.TypeFolder;
import org.rust.lang.core.types.infer.TypeVisitor;
import org.rust.lang.core.types.regions.Region;
import org.rust.lang.core.types.regions.ReStatic;

import java.util.*;

public class TyTraitObject extends Ty {
    @Nonnull
    private final List<BoundElement<RsTraitItem>> myTraits;
    @Nonnull
    private final Region myRegion;
    private final boolean myHasUnresolvedBound;

    public TyTraitObject(@Nonnull List<BoundElement<RsTraitItem>> traits, @Nonnull Region region) {
        this(traits, region, false);
    }

    public TyTraitObject(@Nonnull List<BoundElement<RsTraitItem>> traits, @Nonnull Region region, boolean hasUnresolvedBound) {
        super(KindUtil.mergeElementFlags(new ArrayList<>(traits)) | region.getFlags());
        myTraits = traits;
        myRegion = region;
        myHasUnresolvedBound = hasUnresolvedBound;
    }

    @Nonnull
    public List<BoundElement<RsTraitItem>> getTraits() {
        return myTraits;
    }

    @Nonnull
    public Region getRegion() {
        return myRegion;
    }

    public boolean getHasUnresolvedBound() {
        return myHasUnresolvedBound;
    }

    @Override
    @Nonnull
    public Ty superFoldWith(@Nonnull TypeFolder folder) {
        List<BoundElement<RsTraitItem>> newTraits = new ArrayList<>();
        for (BoundElement<RsTraitItem> trait : myTraits) {
            newTraits.add(trait.foldWith(folder));
        }
        return new TyTraitObject(newTraits, myRegion.superFoldWith(folder), myHasUnresolvedBound);
    }

    @Override
    public boolean superVisitWith(@Nonnull TypeVisitor visitor) {
        for (BoundElement<RsTraitItem> trait : myTraits) {
            if (trait.visitWith(visitor)) return true;
        }
        return myRegion.superVisitWith(visitor);
    }

    @Nonnull
    public static TyTraitObject valueOf(@Nonnull RsTraitItem trait) {
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
