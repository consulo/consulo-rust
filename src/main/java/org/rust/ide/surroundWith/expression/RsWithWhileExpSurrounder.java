/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.surroundWith.expression;

import com.intellij.codeInsight.CodeInsightUtilCore;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiElement;
import org.rust.RsBundle;
import org.rust.lang.core.psi.RsBlock;
import org.rust.lang.core.psi.RsExpr;
import org.rust.lang.core.psi.RsPsiFactory;
import org.rust.lang.core.psi.RsWhileExpr;
import org.rust.lang.core.types.RsTypeUtil;
import org.rust.lang.core.types.ty.TyBool;
import org.rust.lang.core.types.RsTypesUtil;
import org.rust.lang.core.psi.ext.RsFunctionUtil;

public class RsWithWhileExpSurrounder extends RsExpressionSurrounderBase<RsWhileExpr> {

    @Override
    public String getTemplateDescription() {
        return RsBundle.message("action.while.expr.text");
    }

    @Override
    protected RsWhileExpr createTemplate(Project project) {
        return (RsWhileExpr) new RsPsiFactory(project).createExpression("while a {stmt;}");
    }

    @Override
    protected RsExpr getWrappedExpression(RsWhileExpr expression) {
        return expression.getCondition().getExpr();
    }

    @Override
    protected boolean isApplicable(RsExpr expression) {
        return RsTypesUtil.getType(expression) instanceof TyBool;
    }

    @Override
    protected TextRange doPostprocessAndGetSelectionRange(Editor editor, PsiElement expression) {
        if (!(expression instanceof RsWhileExpr)) return null;
        RsBlock block = ((RsWhileExpr) expression).getBlock();
        if (block == null) return null;
        block = CodeInsightUtilCore.forcePsiPostprocessAndRestoreElement(block);
        if (block == null) return null;
        PsiElement rbrace = block.getRbrace();
        if (rbrace == null) {
            throw new IllegalStateException("Incomplete block in while surrounder");
        }

        int offset = block.getLbrace().getTextOffset() + 1;
        editor.getDocument().deleteString(offset, rbrace.getTextOffset());
        return TextRange.from(offset, 0);
    }
}
