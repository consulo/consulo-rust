/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.utils.evaluation;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.rust.lang.core.psi.RsExpr;
import org.rust.lang.core.psi.ext.RsElement;
import org.rust.lang.core.types.consts.Const;
import org.rust.lang.core.types.consts.CtUnknown;
import org.rust.lang.core.types.infer.TypeFoldable;
import org.rust.lang.core.types.ty.Ty;

public final class ConstExprUtil {
    private ConstExprUtil() {}

    /** {@code fun RsExpr.evaluate(expectedTy, resolver = default)}. */
    @Nonnull
    public static Const evaluate(@Nonnull RsExpr expr, @Nonnull Ty expectedTy) {
        return ConstExprEvaluator.evaluate(expr, expectedTy, PathExprResolver.getDefault());
    }

    /** {@code fun RsElement.toConst(expectedTy, resolver)}. */
    @Nonnull
    public static Const toConst(@Nonnull Object value, @Nonnull Ty expectedTy, @Nullable Object resolver) {
        if (!(value instanceof RsElement)) return CtUnknown.INSTANCE;
        PathExprResolver r = resolver instanceof PathExprResolver
            ? (PathExprResolver) resolver
            : PathExprResolver.getDefault();
        return ConstExprEvaluator.toConst((RsElement) value, expectedTy, r);
    }

    /** {@code fun <T> TypeFoldable<T>.tryEvaluate()}. */
    @Nonnull
    public static <T> T tryEvaluate(@Nonnull TypeFoldable<T> foldable) {
        return ConstExprEvaluator.tryEvaluate(foldable);
    }
}
