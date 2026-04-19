/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.fixes;

import org.jetbrains.annotations.NotNull;
import org.rust.lang.core.psi.RsExpr;
import org.rust.lang.core.types.ty.Ty;

/**
 * For the given strExpr converts it to the type ty with strExpr.parse().unwrap() or
 * strExpr.parse()? if possible.
 */
public class ConvertToTyUsingFromStrAndUnpackFix extends ConvertToTyUsingTryTraitAndUnpackFix {

    public ConvertToTyUsingFromStrAndUnpackFix(@NotNull RsExpr strExpr, @NotNull Ty ty, @NotNull Ty errTy) {
        super(strExpr, ty, errTy, "FromStr", (psiFactory, expr, t) ->
            psiFactory.createNoArgsMethodCall(expr, "parse")
        );
    }
}
