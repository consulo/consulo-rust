/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.types.infer;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.rust.lang.core.resolve.ref.MethodResolveVariant;
import org.rust.lang.core.types.Substitution;
import org.rust.lang.core.types.SubstitutionUtil;
import org.rust.lang.core.types.ty.TyFunctionBase;

import java.util.List;

public class InferredMethodCallInfo implements TypeFoldable<InferredMethodCallInfo> {
    @NotNull
    private final List<MethodResolveVariant> myResolveVariants;
    @NotNull
    private Substitution mySubst;
    @Nullable
    private TyFunctionBase myType;

    public InferredMethodCallInfo(@NotNull List<MethodResolveVariant> resolveVariants) {
        this(resolveVariants, SubstitutionUtil.EMPTY_SUBSTITUTION, null);
    }

    public InferredMethodCallInfo(@NotNull List<MethodResolveVariant> resolveVariants,
                                  @NotNull Substitution subst,
                                  @Nullable TyFunctionBase type) {
        myResolveVariants = resolveVariants;
        mySubst = subst;
        myType = type;
    }

    @NotNull
    public List<MethodResolveVariant> getResolveVariants() {
        return myResolveVariants;
    }

    @NotNull
    public Substitution getSubst() {
        return mySubst;
    }

    public void setSubst(@NotNull Substitution subst) {
        mySubst = subst;
    }

    @Nullable
    public TyFunctionBase getType() {
        return myType;
    }

    public void setType(@Nullable TyFunctionBase type) {
        myType = type;
    }

    @NotNull
    public InferredMethodCallInfo copy(@NotNull List<MethodResolveVariant> resolveVariants) {
        return new InferredMethodCallInfo(resolveVariants, mySubst, myType);
    }

    @NotNull
    @Override
    public InferredMethodCallInfo superFoldWith(@NotNull TypeFolder folder) {
        TyFunctionBase foldedType = myType != null ? (TyFunctionBase) myType.foldWith(folder) : null;
        return new InferredMethodCallInfo(myResolveVariants, mySubst.foldValues(folder), foldedType);
    }

    @Override
    public boolean superVisitWith(@NotNull TypeVisitor visitor) {
        boolean result = mySubst.visitValues(visitor);
        if (result) return true;
        return myType != null && myType.visitWith(visitor);
    }
}
