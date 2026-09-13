/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.fixes;

import consulo.codeEditor.Editor;
import consulo.project.Project;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.rust.RsBundle;
import org.rust.lang.core.psi.RsCastExpr;
import org.rust.lang.core.psi.impl.RsPsiFactory;
import org.rust.lang.core.types.RsTypesUtil;
import org.rust.lang.core.types.ty.TyNumeric;
import consulo.localize.LocalizeValue;

public class CompareWithZeroFix extends RsQuickFixBase<RsCastExpr> {

    private CompareWithZeroFix(@Nonnull RsCastExpr expr) {
        super(expr);
    }

    @Nonnull
        public consulo.localize.LocalizeValue getFamilyName() {
        return consulo.localize.LocalizeValue.of(RsBundle.message("intention.family.name.compare.with.zero"));
    }

    @Nonnull
    @Override
    public consulo.localize.LocalizeValue getText() {
        return getFamilyName();
    }

    @Override
    public void invoke(@Nonnull Project project, @Nullable Editor editor, @Nonnull RsCastExpr element) {
        element.replace(new RsPsiFactory(project).createExpression(element.getExpr().getText() + " != 0"));
    }

    @Nullable
    public static CompareWithZeroFix createIfCompatible(@Nonnull RsCastExpr expression) {
        if (RsTypesUtil.getType(expression.getExpr()) instanceof TyNumeric) {
            return new CompareWithZeroFix(expression);
        }
        return null;
    }
}
