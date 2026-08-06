/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.types.infer;

import jakarta.annotation.Nonnull;
import org.rust.lang.core.psi.*;
import org.rust.lang.core.psi.ext.RsElement;
import org.rust.lang.core.types.ty.Ty;
import org.rust.lang.core.types.ty.TyUnknown;

import java.util.List;

public interface RsInferenceData {
    @Nonnull
    List<Adjustment> getExprAdjustments(@Nonnull RsElement expr);

    @Nonnull
    Ty getExprType(@Nonnull RsExpr expr);

    @Nonnull
    ExpectedType getExpectedExprType(@Nonnull RsExpr expr);

    @Nonnull
    Ty getPatType(@Nonnull RsPat pat);

    @Nonnull
    Ty getPatFieldType(@Nonnull RsPatField patField);

    @Nonnull
    List<ResolvedPath> getResolvedPath(@Nonnull RsPathExpr expr);

    boolean isOverloadedOperator(@Nonnull RsExpr expr);

    @Nonnull
    default Ty getBindingType(@Nonnull RsPatBinding binding) {
        Object parent = binding.getParent();
        if (parent instanceof RsPat) {
            return getPatType((RsPat) parent);
        }
        if (parent instanceof RsPatField) {
            return getPatFieldType((RsPatField) parent);
        }
        return TyUnknown.INSTANCE;
    }

    @Nonnull
    default Ty getExprTypeAdjusted(@Nonnull RsExpr expr) {
        List<Adjustment> adjustments = getExprAdjustments(expr);
        if (!adjustments.isEmpty()) {
            return adjustments.get(adjustments.size() - 1).getTarget();
        }
        return getExprType(expr);
    }
}
