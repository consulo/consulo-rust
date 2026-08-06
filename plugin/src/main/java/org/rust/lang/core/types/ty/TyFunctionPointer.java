/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.types.ty;

import jakarta.annotation.Nonnull;
import org.rust.lang.core.types.infer.TypeFolder;

import java.util.Objects;

public class TyFunctionPointer extends TyFunctionBase {
    public TyFunctionPointer(@Nonnull FnSig fnSig) {
        super(fnSig);
    }

    @Override
    @Nonnull
    public Ty superFoldWith(@Nonnull TypeFolder folder) {
        return new TyFunctionPointer(getFnSig().foldWith(folder));
    }

    @Override
    protected boolean isEquivalentToInner(@Nonnull Ty other) {
        if (!(other instanceof TyFunctionPointer)) return false;
        return getFnSig().isEquivalentToInner(((TyFunctionPointer) other).getFnSig());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TyFunctionPointer that = (TyFunctionPointer) o;
        return Objects.equals(getFnSig(), that.getFnSig());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getFnSig());
    }
}
