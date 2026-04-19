/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.mir;

import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.rust.lang.core.mir.schemas.MirOperand;
import org.rust.lang.core.mir.schemas.MirTyples;
import org.rust.lang.core.mir.schemas.MirRvalue;
import org.rust.lang.core.mir.schemas.MirSpan;
import org.rust.lang.core.psi.RsBindingMode;
import org.rust.lang.core.psi.ext.ArithmeticOp;
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

    public static boolean isSigned(@NotNull Ty ty) {
        return MirUtils.isSigned(ty);
    }

    public static long getMinValue(@NotNull TyInteger ty) {
        return MirUtils.getMinValue(ty);
    }

    @NotNull
    public static MirSpan asSpan(@NotNull PsiElement element) {
        return MirUtils.asSpan(element);
    }

    @NotNull
    public static MirSpan asStartSpan(@NotNull PsiElement element) {
        return MirUtils.asStartSpan(element);
    }

    public static boolean needsDrop(@NotNull Ty ty) {
        return MirUtils.needsDrop(ty);
    }

    public static boolean isCheckable(@NotNull ArithmeticOp op) {
        return MirUtils.isCheckable(op);
    }

    @NotNull
    public static MirSpan getSpan(@NotNull Scope scope) {
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

    @NotNull
    public static Mutability getBindingModeMutability(@Nullable RsBindingMode bindingMode) {
        return MirUtils.getBindingModeMutability(bindingMode);
    }

    @Nullable
    public static Scope getVariableScope(@NotNull ScopeTree scopeTree, @NotNull LocalVar variable) {
        return MirUtils.getVariableScope(scopeTree, variable);
    }

    @NotNull
    public static MirRvalue.Cast createCast(
        @Nullable MirTyples.MirCastTy fromTy,
        @Nullable MirTyples.MirCastTy castTy,
        @NotNull MirOperand operand,
        @NotNull Ty ty
    ) {
        return MirUtils.createCast(fromTy, castTy, operand, ty);
    }
}
