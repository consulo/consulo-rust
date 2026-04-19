/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.psi.ext;

import org.jetbrains.annotations.NotNull;
import org.rust.lang.core.psi.RsFunction;
import org.rust.lang.core.psi.RsLambdaExpr;

public final class RsFunctionOrLambdaUtil {
    private RsFunctionOrLambdaUtil() {
    }

    public static boolean isAsync(@NotNull RsFunctionOrLambda fnOrLambda) {
        if (fnOrLambda instanceof RsFunction) {
            return RsFunctionUtil.isAsync((RsFunction) fnOrLambda);
        }
        if (fnOrLambda instanceof RsLambdaExpr) {
            return RsLambdaExprUtil.isAsync((RsLambdaExpr) fnOrLambda);
        }
        throw new IllegalStateException("unreachable: " + fnOrLambda.getClass());
    }
}
