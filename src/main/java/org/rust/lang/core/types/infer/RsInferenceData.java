/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.types.infer;

import org.jetbrains.annotations.NotNull;
import org.rust.lang.core.psi.*;
import org.rust.lang.core.psi.ext.RsElement;
import org.rust.lang.core.types.ty.Ty;
import org.rust.lang.core.types.ty.TyUnknown;

import java.util.List;

public interface RsInferenceData {
    @NotNull
    List<Adjustment> getExprAdjustments(@NotNull RsElement expr);

    @NotNull
    Ty getExprType(@NotNull RsExpr expr);

    @NotNull
    ExpectedType getExpectedExprType(@NotNull RsExpr expr);

    @NotNull
    Ty getPatType(@NotNull RsPat pat);

    @NotNull
    Ty getPatFieldType(@NotNull RsPatField patField);

    @NotNull
    List<ResolvedPath> getResolvedPath(@NotNull RsPathExpr expr);

    boolean isOverloadedOperator(@NotNull RsExpr expr);

    @NotNull
    default Ty getBindingType(@NotNull RsPatBinding binding) {
        Object parent = binding.getParent();
        if (parent instanceof RsPat) {
            return getPatType((RsPat) parent);
        }
        if (parent instanceof RsPatField) {
            return getPatFieldType((RsPatField) parent);
        }
        return TyUnknown.INSTANCE;
    }

    @NotNull
    default Ty getExprTypeAdjusted(@NotNull RsExpr expr) {
        List<Adjustment> adjustments = getExprAdjustments(expr);
        if (!adjustments.isEmpty()) {
            return adjustments.get(adjustments.size() - 1).getTarget();
        }
        return getExprType(expr);
    }
}
