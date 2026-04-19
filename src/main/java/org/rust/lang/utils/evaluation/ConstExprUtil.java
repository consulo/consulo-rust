/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.utils.evaluation;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.rust.lang.core.psi.RsExpr;
import org.rust.lang.core.psi.ext.RsElement;
import org.rust.lang.core.types.consts.Const;
import org.rust.lang.core.types.consts.CtUnknown;
import org.rust.lang.core.types.infer.TypeFoldable;
import org.rust.lang.core.types.ty.Ty;

public final class ConstExprUtil {
    private ConstExprUtil() {}

    /** {@code fun RsExpr.evaluate(expectedTy, resolver = default)}. */
    @NotNull
    public static Const evaluate(@NotNull RsExpr expr, @NotNull Ty expectedTy) {
        return ConstExprEvaluator.evaluate(expr, expectedTy, PathExprResolver.getDefault());
    }

    /** {@code fun RsElement.toConst(expectedTy, resolver)}. */
    @NotNull
    public static Const toConst(@NotNull Object value, @NotNull Ty expectedTy, @Nullable Object resolver) {
        if (!(value instanceof RsElement)) return CtUnknown.INSTANCE;
        PathExprResolver r = resolver instanceof PathExprResolver
            ? (PathExprResolver) resolver
            : PathExprResolver.getDefault();
        return ConstExprEvaluator.toConst((RsElement) value, expectedTy, r);
    }

    /** {@code fun <T> TypeFoldable<T>.tryEvaluate()}. */
    @NotNull
    public static <T> T tryEvaluate(@NotNull TypeFoldable<T> foldable) {
        return ConstExprEvaluator.tryEvaluate(foldable);
    }
}
