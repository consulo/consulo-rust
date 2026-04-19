/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.utils.evaluation;

import org.jetbrains.annotations.NotNull;
import org.rust.lang.core.psi.RsPathExpr;
import org.rust.lang.core.psi.ext.RsElement;
import org.rust.lang.core.types.infer.RsInferenceContext;

/**
 * Utility class for evaluation-related helpers.
 * <p>
 * Provides factory methods for {@link PathExprResolver}.
 *
 * @see PathExprResolver
 */
public final class EvaluationUtil {
    private EvaluationUtil() {
    }

    /**
     * Returns the default {@link PathExprResolver} that resolves path expressions
     * via their reference.
     */
    @NotNull
    public static PathExprResolver getDefaultPathExprResolver() {
        return PathExprResolver.getDefault();
    }

    /**
     * Creates a {@link PathExprResolver} that resolves path expressions
     * using the given inference context.
     */
    @NotNull
    public static PathExprResolver createPathExprResolverFromContext(@NotNull RsInferenceContext ctx) {
        return PathExprResolver.fromContext(ctx);
    }
}
