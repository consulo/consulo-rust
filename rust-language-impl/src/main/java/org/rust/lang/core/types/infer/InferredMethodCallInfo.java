/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.types.infer;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.rust.lang.core.resolve.ref.MethodResolveVariant;
import org.rust.lang.core.types.Substitution;
import org.rust.lang.core.types.SubstitutionUtil;
import org.rust.lang.core.types.ty.TyFunctionBase;

import java.util.List;

public class InferredMethodCallInfo implements TypeFoldable<InferredMethodCallInfo> {
    @Nonnull
    private final List<MethodResolveVariant> myResolveVariants;
    @Nonnull
    private Substitution mySubst;
    @Nullable
    private TyFunctionBase myType;

    public InferredMethodCallInfo(@Nonnull List<MethodResolveVariant> resolveVariants) {
        this(resolveVariants, SubstitutionUtil.EMPTY_SUBSTITUTION, null);
    }

    public InferredMethodCallInfo(@Nonnull List<MethodResolveVariant> resolveVariants,
                                  @Nonnull Substitution subst,
                                  @Nullable TyFunctionBase type) {
        myResolveVariants = resolveVariants;
        mySubst = subst;
        myType = type;
    }

    @Nonnull
    public List<MethodResolveVariant> getResolveVariants() {
        return myResolveVariants;
    }

    @Nonnull
    public Substitution getSubst() {
        return mySubst;
    }

    public void setSubst(@Nonnull Substitution subst) {
        mySubst = subst;
    }

    @Nullable
    public TyFunctionBase getType() {
        return myType;
    }

    public void setType(@Nullable TyFunctionBase type) {
        myType = type;
    }

    @Nonnull
    public InferredMethodCallInfo copy(@Nonnull List<MethodResolveVariant> resolveVariants) {
        return new InferredMethodCallInfo(resolveVariants, mySubst, myType);
    }

    @Nonnull
    @Override
    public InferredMethodCallInfo superFoldWith(@Nonnull TypeFolder folder) {
        TyFunctionBase foldedType = myType != null ? (TyFunctionBase) myType.foldWith(folder) : null;
        return new InferredMethodCallInfo(myResolveVariants, mySubst.foldValues(folder), foldedType);
    }

    @Override
    public boolean superVisitWith(@Nonnull TypeVisitor visitor) {
        boolean result = mySubst.visitValues(visitor);
        if (result) return true;
        return myType != null && myType.visitWith(visitor);
    }
}
