/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.types.ty;

import jakarta.annotation.Nonnull;
import org.rust.lang.core.psi.RsLambdaExpr;
import org.rust.lang.core.types.infer.TypeFolder;

import java.util.Objects;

public class TyClosure extends TyFunctionBase {
    @Nonnull
    private final RsLambdaExpr myDef;

    public TyClosure(@Nonnull RsLambdaExpr def, @Nonnull FnSig fnSig) {
        super(fnSig);
        myDef = def;
    }

    @Nonnull
    public RsLambdaExpr getDef() {
        return myDef;
    }

    @Override
    @Nonnull
    public Ty superFoldWith(@Nonnull TypeFolder folder) {
        return new TyClosure(myDef, getFnSig().foldWith(folder));
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TyClosure that = (TyClosure) o;
        return Objects.equals(myDef, that.myDef) && Objects.equals(getFnSig(), that.getFnSig());
    }

    @Override
    public int hashCode() {
        return Objects.hash(myDef, getFnSig());
    }
}
