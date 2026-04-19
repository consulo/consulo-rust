/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.types.ty;

import org.jetbrains.annotations.NotNull;
import org.rust.lang.core.psi.RsInferType;
import org.rust.lang.core.types.KindUtil;

import java.util.Objects;

public class TyPlaceholder extends Ty {
    @NotNull
    private final RsInferType myOrigin;

    public TyPlaceholder(@NotNull RsInferType origin) {
        super(KindUtil.HAS_TY_PLACEHOLDER_MASK);
        myOrigin = origin;
    }

    @NotNull
    public RsInferType getOrigin() {
        return myOrigin;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TyPlaceholder that = (TyPlaceholder) o;
        return Objects.equals(myOrigin, that.myOrigin);
    }

    @Override
    public int hashCode() {
        return Objects.hash(myOrigin);
    }
}
