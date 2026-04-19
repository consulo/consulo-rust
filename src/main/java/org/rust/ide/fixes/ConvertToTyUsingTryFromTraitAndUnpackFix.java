/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.fixes;

import org.jetbrains.annotations.NotNull;
import org.rust.ide.presentation.TypeRendering;
import org.rust.lang.core.psi.RsExpr;
import org.rust.lang.core.psi.RsPsiFactory;
import org.rust.lang.core.types.ty.Ty;

import java.util.Collections;

/**
 * For the given expr converts it to the type ty with ty::try_from(expr).unwrap() or ty::try_from(expr)? if possible.
 */
public class ConvertToTyUsingTryFromTraitAndUnpackFix extends ConvertToTyUsingTryTraitAndUnpackFix {

    public ConvertToTyUsingTryFromTraitAndUnpackFix(@NotNull RsExpr expr, @NotNull Ty ty, @NotNull Ty errTy) {
        super(expr, ty, errTy, "TryFrom", (psiFactory, e, t) ->
            psiFactory.createAssocFunctionCall(
                TypeRendering.render(t, false),
                "try_from",
                Collections.singletonList(e)
            )
        );
    }
}
