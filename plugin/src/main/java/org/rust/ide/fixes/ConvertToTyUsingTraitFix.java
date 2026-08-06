/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.fixes;

import jakarta.annotation.Nonnull;
import org.rust.lang.core.psi.RsExpr;
import org.rust.lang.core.types.ty.Ty;

public abstract class ConvertToTyUsingTraitFix extends ConvertToTyFix {

    public ConvertToTyUsingTraitFix(@Nonnull RsExpr expr, @Nonnull String tyName, @Nonnull String traitName) {
        super(expr, tyName, "`" + traitName + "` trait");
    }

    public ConvertToTyUsingTraitFix(@Nonnull RsExpr expr, @Nonnull Ty ty, @Nonnull String traitName) {
        super(expr, ty, "`" + traitName + "` trait");
    }
}
