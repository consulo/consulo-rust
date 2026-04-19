/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.fixes;

import org.jetbrains.annotations.NotNull;
import org.rust.RsBundle;
import org.rust.ide.presentation.TypeRendering;
import org.rust.lang.core.psi.RsExpr;
import org.rust.lang.core.types.ty.Ty;

public abstract class ConvertToTyFix extends RsQuickFixBase<RsExpr> {
    private final String myTyName;
    private final String myConvertSubject;

    public ConvertToTyFix(@NotNull RsExpr expr, @NotNull String tyName, @NotNull String convertSubject) {
        super(expr);
        this.myTyName = tyName;
        this.myConvertSubject = convertSubject;
    }

    public ConvertToTyFix(@NotNull RsExpr expr, @NotNull Ty ty, @NotNull String convertSubject) {
        this(expr, TypeRendering.render(ty), convertSubject);
    }

    @NotNull
    @Override
    public String getFamilyName() {
        return RsBundle.message("intention.family.name.convert.to.type");
    }

    @NotNull
    @Override
    public String getText() {
        return RsBundle.message("intention.name.convert.to.using", myTyName, myConvertSubject);
    }
}
