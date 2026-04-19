/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.surroundWith.expression;

import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiElement;
import org.rust.RsBundle;
import org.rust.lang.core.psi.RsExpr;
import org.rust.lang.core.psi.RsParenExpr;
import org.rust.lang.core.psi.RsPsiFactory;

public class RsWithParenthesesSurrounder extends RsExpressionSurrounderBase<RsParenExpr> {

    @Override
    public String getTemplateDescription() {
        return RsBundle.message("action.expr.text");
    }

    @Override
    protected RsParenExpr createTemplate(Project project) {
        return (RsParenExpr) new RsPsiFactory(project).createExpression("(a)");
    }

    @Override
    protected RsExpr getWrappedExpression(RsParenExpr expression) {
        return expression.getExpr();
    }

    @Override
    protected boolean isApplicable(RsExpr expression) {
        return true;
    }

    @Override
    protected TextRange doPostprocessAndGetSelectionRange(Editor editor, PsiElement expression) {
        int offset = expression.getTextRange().getEndOffset();
        return TextRange.from(offset, 0);
    }
}
