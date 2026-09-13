/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.fixes;

import jakarta.annotation.Nonnull;
import org.rust.RsBundle;
import org.rust.lang.core.presentation.TypeRendering;
import org.rust.lang.core.psi.RsExpr;
import org.rust.lang.core.types.ty.Ty;
import consulo.localize.LocalizeValue;

public abstract class ConvertToTyFix extends RsQuickFixBase<RsExpr> {
    private final String myTyName;
    private final String myConvertSubject;

    public ConvertToTyFix(@Nonnull RsExpr expr, @Nonnull String tyName, @Nonnull String convertSubject) {
        super(expr);
        this.myTyName = tyName;
        this.myConvertSubject = convertSubject;
    }

    public ConvertToTyFix(@Nonnull RsExpr expr, @Nonnull Ty ty, @Nonnull String convertSubject) {
        this(expr, TypeRendering.render(ty), convertSubject);
    }

    @Nonnull
        public consulo.localize.LocalizeValue getFamilyName() {
        return consulo.localize.LocalizeValue.of(RsBundle.message("intention.family.name.convert.to.type"));
    }

    @Nonnull
    @Override
    public consulo.localize.LocalizeValue getText() {
        return consulo.localize.LocalizeValue.of(RsBundle.message("intention.name.convert.to.using", myTyName, myConvertSubject));
    }
}
