/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.surroundWith.expression;

import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiElement;
import org.rust.lang.core.psi.*;
import org.rust.lang.core.types.RsTypeUtil;
import org.rust.lang.core.types.ty.TyBool;
import org.rust.lang.core.types.RsTypesUtil;

public class RsWithNotSurrounder extends RsExpressionSurrounderBase<RsUnaryExpr> {

    @Override
    public String getTemplateDescription() {
        return "!(expr)";
    }

    @Override
    protected RsUnaryExpr createTemplate(Project project) {
        return (RsUnaryExpr) new RsPsiFactory(project).createExpression("!(a)");
    }

    @Override
    protected RsExpr getWrappedExpression(RsUnaryExpr expression) {
        return ((RsParenExpr) expression.getExpr()).getExpr();
    }

    @Override
    protected boolean isApplicable(RsExpr expression) {
        return RsTypesUtil.getType(expression) instanceof TyBool;
    }

    @Override
    protected TextRange doPostprocessAndGetSelectionRange(Editor editor, PsiElement expression) {
        int offset = expression.getTextRange().getEndOffset();
        return TextRange.from(offset, 0);
    }
}
