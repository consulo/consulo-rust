/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.fixes;

import org.jetbrains.annotations.NotNull;
import org.rust.lang.core.psi.RsExpr;
import org.rust.lang.core.types.ty.Ty;

/**
 * For the given strExpr converts it to the type Result with strExpr.parse().
 */
public class ConvertToTyUsingFromStrFix extends ConvertToTyUsingTryTraitFix {

    public ConvertToTyUsingFromStrFix(@NotNull RsExpr strExpr, @NotNull Ty ty) {
        super(strExpr, ty, "FromStr", (psiFactory, expr, t) ->
            psiFactory.createNoArgsMethodCall(expr, "parse")
        );
    }
}
