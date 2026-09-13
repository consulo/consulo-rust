/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.surroundWith.expression;

import consulo.codeEditor.Editor;
import consulo.project.Project;
import consulo.document.util.TextRange;
import consulo.language.psi.PsiElement;
import org.rust.lang.core.psi.*;
import org.rust.lang.core.types.ty.TyBool;
import org.rust.lang.core.types.RsTypesUtil;
import consulo.localize.LocalizeValue;
import org.rust.lang.core.psi.impl.*;

public class RsWithNotSurrounder extends RsExpressionSurrounderBase<RsUnaryExpr> {

    @Override
    public consulo.localize.LocalizeValue getTemplateDescription() {
        return consulo.localize.LocalizeValue.of("!(expr)");
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
