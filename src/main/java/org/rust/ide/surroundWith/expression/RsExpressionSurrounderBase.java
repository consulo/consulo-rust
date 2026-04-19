/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.surroundWith.expression;

import com.intellij.lang.surroundWith.Surrounder;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiElement;
import org.rust.lang.core.psi.RsExpr;

public abstract class RsExpressionSurrounderBase<E extends RsExpr> implements Surrounder {

    protected abstract E createTemplate(Project project);

    protected abstract RsExpr getWrappedExpression(E expression);

    protected abstract boolean isApplicable(RsExpr expression);

    protected abstract TextRange doPostprocessAndGetSelectionRange(Editor editor, PsiElement expression);

    @Override
    public final boolean isApplicable(PsiElement[] elements) {
        RsExpr expression = targetExpr(elements);
        return expression != null && isApplicable(expression);
    }

    @Override
    public final TextRange surroundElements(Project project, Editor editor, PsiElement[] elements) {
        RsExpr expression = targetExpr(elements);
        if (expression == null) {
            throw new IllegalArgumentException("Expected single RsExpr element");
        }
        E templateExpr = createTemplate(project);

        getWrappedExpression(templateExpr).replace(expression);
        PsiElement newExpression = expression.replace(templateExpr);

        return doPostprocessAndGetSelectionRange(editor, newExpression);
    }

    @SuppressWarnings("unchecked")
    private RsExpr targetExpr(PsiElement[] elements) {
        if (elements.length != 1) return null;
        if (elements[0] instanceof RsExpr) return (RsExpr) elements[0];
        return null;
    }
}
