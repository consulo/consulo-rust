/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.types.ty;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.rust.lang.core.psi.RsSelfParameter;
import org.rust.lang.core.psi.RsTypeReference;
import org.rust.lang.core.types.RsCallable;
import org.rust.lang.core.types.RsTypesUtil;
import org.rust.lang.core.types.infer.TypeFoldable;
import org.rust.lang.core.types.infer.TypeFolder;
import org.rust.lang.core.types.infer.TypeVisitor;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class FnSig implements TypeFoldable<FnSig> {
    @NotNull
    private final List<Ty> myParamTypes;
    @NotNull
    private final Ty myRetType;
    @NotNull
    private final Unsafety myUnsafety;

    public FnSig(@NotNull List<Ty> paramTypes, @NotNull Ty retType) {
        this(paramTypes, retType, Unsafety.Normal);
    }

    public FnSig(@NotNull List<Ty> paramTypes, @NotNull Ty retType, @NotNull Unsafety unsafety) {
        myParamTypes = paramTypes;
        myRetType = retType;
        myUnsafety = unsafety;
    }

    @NotNull
    public List<Ty> getParamTypes() {
        return myParamTypes;
    }

    @NotNull
    public Ty getRetType() {
        return myRetType;
    }

    @NotNull
    public Unsafety getUnsafety() {
        return myUnsafety;
    }

    @NotNull
    public FnSig copy(@NotNull Unsafety unsafety) {
        return new FnSig(myParamTypes, myRetType, unsafety);
    }

    @Override
    @NotNull
    public FnSig superFoldWith(@NotNull TypeFolder folder) {
        List<Ty> newParams = new ArrayList<>();
        for (Ty param : myParamTypes) {
            newParams.add(param.foldWith(folder));
        }
        return new FnSig(newParams, myRetType.foldWith(folder), myUnsafety);
    }

    @Override
    public boolean superVisitWith(@NotNull TypeVisitor visitor) {
        for (Ty param : myParamTypes) {
            if (param.visitWith(visitor)) return true;
        }
        return myRetType.visitWith(visitor);
    }

    public boolean isEquivalentToInner(@NotNull FnSig other) {
        if (this == other) return true;
        if (myParamTypes.size() != other.myParamTypes.size()) return false;
        for (int i = 0; i < myParamTypes.size(); i++) {
            if (!myParamTypes.get(i).isEquivalentTo(other.myParamTypes.get(i))) return false;
        }
        if (!myRetType.isEquivalentTo(other.myRetType)) return false;
        return myUnsafety == other.myUnsafety;
    }

    @NotNull
    public static FnSig of(@NotNull RsCallable callable) {
        List<Ty> paramTypes = new ArrayList<>();
        RsSelfParameter self = callable.getSelfParameter();
        if (self != null) {
            @Nullable RsTypeReference selfTypeRef = self.getTypeReference();
            paramTypes.add(selfTypeRef != null ? RsTypesUtil.getRawType(selfTypeRef) : TyUnknown.INSTANCE);
        }
        paramTypes.addAll(callable.getParameterTypes());
        Ty retType = callable.getRawReturnType();
        return new FnSig(paramTypes, retType, Unsafety.fromBoolean(callable.isActuallyUnsafe()));
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        FnSig fnSig = (FnSig) o;
        return Objects.equals(myParamTypes, fnSig.myParamTypes)
            && Objects.equals(myRetType, fnSig.myRetType)
            && myUnsafety == fnSig.myUnsafety;
    }

    @Override
    public int hashCode() {
        return Objects.hash(myParamTypes, myRetType, myUnsafety);
    }
}
