/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.psi.ext;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.rust.lang.core.psi.RsEnvMacroArgument;
import org.rust.lang.core.psi.RsExpr;

import java.util.List;

public final class RsEnvMacroArgumentUtil {
    private RsEnvMacroArgumentUtil() {
    }

    @Nullable
    public static RsExpr getVariableNameExpr(@NotNull RsEnvMacroArgument argument) {
        List<RsExpr> exprList = argument.getExprList();
        return exprList.isEmpty() ? null : exprList.get(0);
    }
}
