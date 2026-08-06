/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.types.ty;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.rust.lang.core.psi.RsTraitItem;
import org.rust.lang.core.psi.RsTraitType;
import org.rust.lang.core.types.*;
import org.rust.lang.core.types.infer.TypeFolder;
import org.rust.lang.core.types.infer.TypeVisitor;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class TyAnon extends Ty {
    @Nullable
    private final RsTraitType myDefinition;
    @Nonnull
    private final List<BoundElement<RsTraitItem>> myTraits;

    public TyAnon(@Nullable RsTraitType definition, @Nonnull List<BoundElement<RsTraitItem>> traits) {
        super(KindUtil.HAS_TY_OPAQUE_MASK | KindUtil.mergeElementFlags(new ArrayList<>(traits)));
        myDefinition = definition;
        myTraits = traits;
    }

    @Nullable
    public RsTraitType getDefinition() {
        return myDefinition;
    }

    @Nonnull
    public List<BoundElement<RsTraitItem>> getTraits() {
        return myTraits;
    }

    @Override
    @Nonnull
    public Ty superFoldWith(@Nonnull TypeFolder folder) {
        List<BoundElement<RsTraitItem>> newTraits = new ArrayList<>();
        for (BoundElement<RsTraitItem> trait : myTraits) {
            newTraits.add(trait.foldWith(folder));
        }
        return new TyAnon(myDefinition, newTraits);
    }

    @Override
    public boolean superVisitWith(@Nonnull TypeVisitor visitor) {
        for (BoundElement<RsTraitItem> trait : myTraits) {
            if (trait.visitWith(visitor)) return true;
        }
        return false;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TyAnon tyAnon = (TyAnon) o;
        return Objects.equals(myDefinition, tyAnon.myDefinition) && Objects.equals(myTraits, tyAnon.myTraits);
    }

    @Override
    public int hashCode() {
        return Objects.hash(myDefinition, myTraits);
    }
}
