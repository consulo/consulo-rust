/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.types.ty;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
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
    @Nonnull
    private final List<Ty> myParamTypes;
    @Nonnull
    private final Ty myRetType;
    @Nonnull
    private final Unsafety myUnsafety;

    public FnSig(@Nonnull List<Ty> paramTypes, @Nonnull Ty retType) {
        this(paramTypes, retType, Unsafety.Normal);
    }

    public FnSig(@Nonnull List<Ty> paramTypes, @Nonnull Ty retType, @Nonnull Unsafety unsafety) {
        myParamTypes = paramTypes;
        myRetType = retType;
        myUnsafety = unsafety;
    }

    @Nonnull
    public List<Ty> getParamTypes() {
        return myParamTypes;
    }

    @Nonnull
    public Ty getRetType() {
        return myRetType;
    }

    @Nonnull
    public Unsafety getUnsafety() {
        return myUnsafety;
    }

    @Nonnull
    public FnSig copy(@Nonnull Unsafety unsafety) {
        return new FnSig(myParamTypes, myRetType, unsafety);
    }

    @Override
    @Nonnull
    public FnSig superFoldWith(@Nonnull TypeFolder folder) {
        List<Ty> newParams = new ArrayList<>();
        for (Ty param : myParamTypes) {
            newParams.add(param.foldWith(folder));
        }
        return new FnSig(newParams, myRetType.foldWith(folder), myUnsafety);
    }

    @Override
    public boolean superVisitWith(@Nonnull TypeVisitor visitor) {
        for (Ty param : myParamTypes) {
            if (param.visitWith(visitor)) return true;
        }
        return myRetType.visitWith(visitor);
    }

    public boolean isEquivalentToInner(@Nonnull FnSig other) {
        if (this == other) return true;
        if (myParamTypes.size() != other.myParamTypes.size()) return false;
        for (int i = 0; i < myParamTypes.size(); i++) {
            if (!myParamTypes.get(i).isEquivalentTo(other.myParamTypes.get(i))) return false;
        }
        if (!myRetType.isEquivalentTo(other.myRetType)) return false;
        return myUnsafety == other.myUnsafety;
    }

    @Nonnull
    public static FnSig of(@Nonnull RsCallable callable) {
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
