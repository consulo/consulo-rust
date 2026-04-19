/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.fixes;

import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.rust.RsBundle;
import org.rust.lang.core.psi.RsCastExpr;
import org.rust.lang.core.psi.RsPsiFactory;
import org.rust.lang.core.types.RsTypesUtil;
import org.rust.lang.core.types.ty.TyNumeric;

public class CompareWithZeroFix extends RsQuickFixBase<RsCastExpr> {

    private CompareWithZeroFix(@NotNull RsCastExpr expr) {
        super(expr);
    }

    @NotNull
    @Override
    public String getFamilyName() {
        return RsBundle.message("intention.family.name.compare.with.zero");
    }

    @NotNull
    @Override
    public String getText() {
        return getFamilyName();
    }

    @Override
    public void invoke(@NotNull Project project, @Nullable Editor editor, @NotNull RsCastExpr element) {
        element.replace(new RsPsiFactory(project).createExpression(element.getExpr().getText() + " != 0"));
    }

    @Nullable
    public static CompareWithZeroFix createIfCompatible(@NotNull RsCastExpr expression) {
        if (RsTypesUtil.getType(expression.getExpr()) instanceof TyNumeric) {
            return new CompareWithZeroFix(expression);
        }
        return null;
    }
}
