/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.mir;

import consulo.language.psi.PsiElement;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.rust.lang.core.mir.schemas.MirOperand;
import org.rust.lang.core.mir.schemas.MirTyples;
import org.rust.lang.core.mir.schemas.MirRvalue;
import org.rust.lang.core.mir.schemas.MirSpan;
import org.rust.lang.core.psi.RsBindingMode;
import org.rust.lang.core.psi.ext.impl.ArithmeticOp;
import org.rust.lang.core.thir.LocalVar;
import org.rust.lang.core.types.regions.Scope;
import org.rust.lang.core.types.regions.ScopeTree;
import org.rust.lang.core.types.ty.*;
import org.rust.lang.core.types.ty.Mutability;

/**
 * Delegates to {@link MirUtils} which contains the full implementation.
 */
public final class Utils {
    private Utils() {
    }

    public static boolean isSigned(@Nonnull Ty ty) {
        return MirUtils.isSigned(ty);
    }

    public static long getMinValue(@Nonnull TyInteger ty) {
        return MirUtils.getMinValue(ty);
    }

    @Nonnull
    public static MirSpan asSpan(@Nonnull PsiElement element) {
        return MirUtils.asSpan(element);
    }

    @Nonnull
    public static MirSpan asStartSpan(@Nonnull PsiElement element) {
        return MirUtils.asStartSpan(element);
    }

    public static boolean needsDrop(@Nonnull Ty ty) {
        return MirUtils.needsDrop(ty);
    }

    public static boolean isCheckable(@Nonnull ArithmeticOp op) {
        return MirUtils.isCheckable(op);
    }

    @Nonnull
    public static MirSpan getSpan(@Nonnull Scope scope) {
        return MirUtils.getSpan(scope);
    }

    @Nullable
    public static PsiElement getBindingModeMut(@Nullable RsBindingMode bindingMode) {
        return MirUtils.getBindingModeMut(bindingMode);
    }

    @Nullable
    public static PsiElement getBindingModeRef(@Nullable RsBindingMode bindingMode) {
        return MirUtils.getBindingModeRef(bindingMode);
    }

    @Nonnull
    public static Mutability getBindingModeMutability(@Nullable RsBindingMode bindingMode) {
        return MirUtils.getBindingModeMutability(bindingMode);
    }

    @Nullable
    public static Scope getVariableScope(@Nonnull ScopeTree scopeTree, @Nonnull LocalVar variable) {
        return MirUtils.getVariableScope(scopeTree, variable);
    }

    @Nonnull
    public static MirRvalue.Cast createCast(
        @Nullable MirTyples.MirCastTy fromTy,
        @Nullable MirTyples.MirCastTy castTy,
        @Nonnull MirOperand operand,
        @Nonnull Ty ty
    ) {
        return MirUtils.createCast(fromTy, castTy, operand, ty);
    }
}
