/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.types.ty;

import org.jetbrains.annotations.NotNull;
import org.rust.lang.core.types.KindUtil;
import org.rust.lang.core.types.infer.TypeFolder;
import org.rust.lang.core.types.infer.TypeVisitor;

import java.util.List;

public abstract class TyFunctionBase extends Ty {
    @NotNull
    private final FnSig myFnSig;

    protected TyFunctionBase(@NotNull FnSig fnSig) {
        super(KindUtil.mergeFlags(fnSig.getParamTypes()) | fnSig.getRetType().getFlags());
        myFnSig = fnSig;
    }

    protected TyFunctionBase(@NotNull FnSig fnSig, int additionalFlags) {
        super(KindUtil.mergeFlags(fnSig.getParamTypes()) | fnSig.getRetType().getFlags() | additionalFlags);
        myFnSig = fnSig;
    }

    @NotNull
    public FnSig getFnSig() {
        return myFnSig;
    }

    @NotNull
    public List<Ty> getParamTypes() {
        return myFnSig.getParamTypes();
    }

    @NotNull
    public Ty getRetType() {
        return myFnSig.getRetType();
    }

    @NotNull
    public Unsafety getUnsafety() {
        return myFnSig.getUnsafety();
    }

    @Override
    public boolean superVisitWith(@NotNull TypeVisitor visitor) {
        return myFnSig.superVisitWith(visitor);
    }
}
