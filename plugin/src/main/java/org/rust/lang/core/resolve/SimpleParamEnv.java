/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.resolve;

import jakarta.annotation.Nonnull;
import org.rust.lang.core.psi.RsTraitItem;
import org.rust.lang.core.types.BoundElement;
import org.rust.lang.core.types.TraitRef;
import org.rust.lang.core.types.ty.Ty;

import java.util.*;
import java.util.stream.Collectors;

public class SimpleParamEnv implements ParamEnv {
    @Nonnull
    private final List<TraitRef> callerBounds;

    public SimpleParamEnv(@Nonnull List<TraitRef> callerBounds) {
        this.callerBounds = callerBounds;
    }

    @Nonnull
    @Override
    public Sequence<BoundElement<RsTraitItem>> boundsFor(@Nonnull Ty ty) {
        List<BoundElement<RsTraitItem>> result = callerBounds.stream()
            .filter(ref -> ref.getSelfTy().isEquivalentTo(ty))
            .map(TraitRef::getTrait)
            .collect(Collectors.toList());
        return result::iterator;
    }
}
