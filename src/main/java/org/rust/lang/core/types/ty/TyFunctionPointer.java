/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.types.ty;

import org.jetbrains.annotations.NotNull;
import org.rust.lang.core.types.infer.TypeFolder;

import java.util.Objects;

public class TyFunctionPointer extends TyFunctionBase {
    public TyFunctionPointer(@NotNull FnSig fnSig) {
        super(fnSig);
    }

    @Override
    @NotNull
    public Ty superFoldWith(@NotNull TypeFolder folder) {
        return new TyFunctionPointer(getFnSig().foldWith(folder));
    }

    @Override
    protected boolean isEquivalentToInner(@NotNull Ty other) {
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
