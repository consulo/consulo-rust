/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.utils.evaluation;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.rust.lang.core.psi.RsPathExpr;
import org.rust.lang.core.psi.ext.RsElement;
import org.rust.lang.core.types.infer.RsInferenceContext;

import java.util.List;
import java.util.function.Function;

public class PathExprResolver implements Function<RsPathExpr, RsElement> {
    @NotNull
    private final Function<RsPathExpr, RsElement> myResolver;

    public PathExprResolver(@NotNull Function<RsPathExpr, RsElement> resolver) {
        myResolver = resolver;
    }

    @Nullable
    public RsElement invoke(@NotNull RsPathExpr expr) {
        return myResolver.apply(expr);
    }

    @Override
    public RsElement apply(RsPathExpr rsPathExpr) {
        return invoke(rsPathExpr);
    }

    @NotNull
    private static final PathExprResolver DEFAULT = new PathExprResolver(expr -> {
        if (expr.getPath().getReference() != null) {
            return (RsElement) expr.getPath().getReference().resolve();
        }
        return null;
    });

    @NotNull
    public static PathExprResolver getDefault() {
        return DEFAULT;
    }

    @NotNull
    public static PathExprResolver fromContext(@NotNull RsInferenceContext ctx) {
        return new PathExprResolver(expr -> {
            List<?> resolved = ctx.getResolvedPath(expr);
            if (resolved != null && resolved.size() == 1) {
                Object entry = resolved.get(0);
                // Access element field from the resolved path entry
                try {
                    java.lang.reflect.Method method = entry.getClass().getMethod("getElement");
                    return (RsElement) method.invoke(entry);
                } catch (Exception e) {
                    return null;
                }
            }
            return null;
        });
    }
}
