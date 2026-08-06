/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.types.ty;

import jakarta.annotation.Nonnull;
import org.rust.lang.core.types.KindUtil;
import org.rust.lang.core.types.infer.TypeFolder;
import org.rust.lang.core.types.infer.TypeVisitor;

import java.util.List;

public abstract class TyFunctionBase extends Ty {
    @Nonnull
    private final FnSig myFnSig;

    protected TyFunctionBase(@Nonnull FnSig fnSig) {
        super(KindUtil.mergeFlags(fnSig.getParamTypes()) | fnSig.getRetType().getFlags());
        myFnSig = fnSig;
    }

    protected TyFunctionBase(@Nonnull FnSig fnSig, int additionalFlags) {
        super(KindUtil.mergeFlags(fnSig.getParamTypes()) | fnSig.getRetType().getFlags() | additionalFlags);
        myFnSig = fnSig;
    }

    @Nonnull
    public FnSig getFnSig() {
        return myFnSig;
    }

    @Nonnull
    public List<Ty> getParamTypes() {
        return myFnSig.getParamTypes();
    }

    @Nonnull
    public Ty getRetType() {
        return myFnSig.getRetType();
    }

    @Nonnull
    public Unsafety getUnsafety() {
        return myFnSig.getUnsafety();
    }

    @Override
    public boolean superVisitWith(@Nonnull TypeVisitor visitor) {
        return myFnSig.superVisitWith(visitor);
    }
}
