/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.fixes;

import org.jetbrains.annotations.NotNull;
import org.rust.lang.core.psi.RsExpr;
import org.rust.lang.core.types.ty.Ty;

public abstract class ConvertToTyUsingTraitFix extends ConvertToTyFix {

    public ConvertToTyUsingTraitFix(@NotNull RsExpr expr, @NotNull String tyName, @NotNull String traitName) {
        super(expr, tyName, "`" + traitName + "` trait");
    }

    public ConvertToTyUsingTraitFix(@NotNull RsExpr expr, @NotNull Ty ty, @NotNull String traitName) {
        super(expr, ty, "`" + traitName + "` trait");
    }
}
