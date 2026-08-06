/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.types.ty;

import jakarta.annotation.Nonnull;
import org.rust.lang.core.types.RsCallable;
import org.rust.lang.core.types.infer.TypeFolder;

import java.util.Objects;

public class TyFunctionDef extends TyFunctionBase {
    @Nonnull
    private final RsCallable myDef;

    public TyFunctionDef(@Nonnull RsCallable def, @Nonnull FnSig fnSig) {
        super(fnSig);
        myDef = def;
    }

    @Nonnull
    public RsCallable getDef() {
        return myDef;
    }

    @Override
    @Nonnull
    public Ty superFoldWith(@Nonnull TypeFolder folder) {
        return new TyFunctionDef(myDef, getFnSig().foldWith(folder));
    }

    @Override
    protected boolean isEquivalentToInner(@Nonnull Ty other) {
        if (!(other instanceof TyFunctionDef)) return false;
        TyFunctionDef otherDef = (TyFunctionDef) other;
        return myDef.equals(otherDef.myDef) && getFnSig().isEquivalentToInner(otherDef.getFnSig());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TyFunctionDef that = (TyFunctionDef) o;
        return Objects.equals(myDef, that.myDef) && Objects.equals(getFnSig(), that.getFnSig());
    }

    @Override
    public int hashCode() {
        return Objects.hash(myDef, getFnSig());
    }
}
