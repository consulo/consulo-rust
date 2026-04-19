/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.types.ty;

import org.jetbrains.annotations.NotNull;
import org.rust.lang.core.types.infer.TypeFolder;
import org.rust.lang.core.types.infer.TypeVisitor;

import java.util.Objects;

public class TySlice extends Ty {
    @NotNull
    private final Ty myElementType;

    public TySlice(@NotNull Ty elementType) {
        super(elementType.getFlags());
        myElementType = elementType;
    }

    @NotNull
    public Ty getElementType() {
        return myElementType;
    }

    @Override
    @NotNull
    public Ty superFoldWith(@NotNull TypeFolder folder) {
        return new TySlice(myElementType.foldWith(folder));
    }

    @Override
    public boolean superVisitWith(@NotNull TypeVisitor visitor) {
        return myElementType.visitWith(visitor);
    }

    @Override
    protected boolean isEquivalentToInner(@NotNull Ty other) {
        if (!(other instanceof TySlice)) return false;
        return myElementType.isEquivalentTo(((TySlice) other).myElementType);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TySlice that = (TySlice) o;
        return Objects.equals(myElementType, that.myElementType);
    }

    @Override
    public int hashCode() {
        return Objects.hash(myElementType);
    }
}
