/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.resolve;

import org.jetbrains.annotations.NotNull;
import org.rust.lang.core.psi.RsTraitItem;
import org.rust.lang.core.types.BoundElement;
import org.rust.lang.core.types.TraitRef;
import org.rust.lang.core.types.ty.Ty;

import java.util.*;
import java.util.stream.Collectors;

public class SimpleParamEnv implements ParamEnv {
    @NotNull
    private final List<TraitRef> callerBounds;

    public SimpleParamEnv(@NotNull List<TraitRef> callerBounds) {
        this.callerBounds = callerBounds;
    }

    @NotNull
    @Override
    public Sequence<BoundElement<RsTraitItem>> boundsFor(@NotNull Ty ty) {
        List<BoundElement<RsTraitItem>> result = callerBounds.stream()
            .filter(ref -> ref.getSelfTy().isEquivalentTo(ty))
            .map(TraitRef::getTrait)
            .collect(Collectors.toList());
        return result::iterator;
    }
}
