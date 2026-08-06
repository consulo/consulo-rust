/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.mir;

import consulo.language.psi.PsiElement;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.rust.lang.core.mir.schemas.*;
import org.rust.lang.core.psi.RsBindingMode;
import org.rust.lang.core.psi.ext.ArithmeticOp;
import org.rust.lang.core.thir.LocalVar;
import org.rust.lang.core.types.regions.Scope;
import org.rust.lang.core.types.regions.ScopeTree;
import org.rust.lang.core.types.ty.*;

public final class MirUtils {
    private MirUtils() {
    }

    public static boolean isSigned(@Nonnull Ty ty) {
        return ty instanceof TyInteger.I8
            || ty instanceof TyInteger.I16
            || ty instanceof TyInteger.I32
            || ty instanceof TyInteger.I64
            || ty instanceof TyInteger.I128
            || ty instanceof TyInteger.ISize;
    }

    public static long getMinValue(@Nonnull TyInteger ty) {
        assert isSigned(ty);
        // TODO
        return Integer.MIN_VALUE;
    }

    @Nonnull
    public static MirSpan asSpan(@Nonnull PsiElement element) {
        return new MirSpan.Full(element);
    }

    @Nonnull
    public static MirSpan asStartSpan(@Nonnull PsiElement element) {
        return new MirSpan.Start(element);
    }

    public static boolean needsDrop(@Nonnull Ty ty) {
        // TODO: it's just a dummy impl
        if (ty instanceof TyPrimitive) return false;
        if (ty instanceof TyTuple) {
            for (Ty type : ((TyTuple) ty).getTypes()) {
                if (needsDrop(type)) return true;
            }
            return false;
        }
        if (ty instanceof TyAdt) return false; // TODO: usually not false actually
        if (ty instanceof TyReference) return false;
        if (ty instanceof TyFunctionBase) return false;
        return true;
    }

    public static boolean isCheckable(@Nonnull ArithmeticOp op) {
        return op == ArithmeticOp.ADD
            || op == ArithmeticOp.SUB
            || op == ArithmeticOp.MUL
            || op == ArithmeticOp.SHL
            || op == ArithmeticOp.SHR;
    }

    @Nonnull
    public static MirSpan getSpan(@Nonnull Scope scope) {
        // TODO: it can be more complicated in case of remainder
        return asSpan(scope.getElement());
    }

    @Nullable
    public static Scope getVariableScope(@Nonnull ScopeTree scopeTree, @Nonnull LocalVar variable) {
        if (variable instanceof LocalVar.FromPatBinding) {
            return scopeTree.getVariableScope(((LocalVar.FromPatBinding) variable).pat);
        } else if (variable instanceof LocalVar.FromSelfParameter) {
            return scopeTree.getVariableScope(((LocalVar.FromSelfParameter) variable).self);
        }
        return null;
    }

    @Nullable
    public static PsiElement getBindingModeMut(@Nullable RsBindingMode bindingMode) {
        return bindingMode != null ? bindingMode.getMut() : null;
    }

    @Nullable
    public static PsiElement getBindingModeRef(@Nullable RsBindingMode bindingMode) {
        return bindingMode != null ? bindingMode.getRef() : null;
    }

    @Nonnull
    public static org.rust.lang.core.types.ty.Mutability getBindingModeMutability(@Nullable RsBindingMode bindingMode) {
        if (bindingMode != null && bindingMode.getMut() != null) {
            return org.rust.lang.core.types.ty.Mutability.MUTABLE;
        }
        return org.rust.lang.core.types.ty.Mutability.IMMUTABLE;
    }

    // compiler uses ty and calls MirCastTy.from two times, but let's not
    @Nonnull
    public static MirRvalue.Cast createCast(
        @Nullable MirTyples.MirCastTy fromTy,
        @Nullable MirTyples.MirCastTy castTy,
        @Nonnull MirOperand operand,
        @Nonnull Ty ty
    ) {
        if (fromTy instanceof MirTyples.MirCastTy.Int && castTy instanceof MirTyples.MirCastTy.Int) {
            return new MirRvalue.Cast.IntToInt(operand, ty);
        } else {
            throw new UnsupportedOperationException("TODO");
        }
    }
}

/**
 * This class exists because in case of `let x = 3` there is no binding mode created in PSI.
 */
