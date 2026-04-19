/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.surroundWith.statement;

import com.intellij.codeInsight.CodeInsightUtilBase;
import com.intellij.lang.surroundWith.Surrounder;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Pair;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiElement;
import org.rust.ide.surroundWith.RsSurroundWithUtils;
import org.rust.lang.core.psi.RsBlock;
import org.rust.lang.core.psi.RsExpr;

public abstract class RsStatementsSurrounderBase<T extends RsExpr> implements Surrounder {

    protected abstract Pair<T, RsBlock> createTemplate(Project project);

    public abstract static class SimpleBlock<T extends RsExpr> extends RsStatementsSurrounderBase<T> {
        @Override
        public final TextRange surroundElements(Project project, Editor editor, PsiElement[] elements) {
            T template = surroundWithTemplate(project, elements);
            return TextRange.from(template.getFirstChild().getTextRange().getEndOffset(), 0);
        }
    }

    public abstract static class BlockWithCondition<T extends RsExpr> extends RsStatementsSurrounderBase<T> {
        protected abstract TextRange conditionRange(T expression);

        @Override
        public final TextRange surroundElements(Project project, Editor editor, PsiElement[] elements) {
            T template = surroundWithTemplate(project, elements);
            template = CodeInsightUtilBase.forcePsiPostprocessAndRestoreElement(template);
            if (template == null) return null;

            TextRange range = conditionRange(template);
            editor.getDocument().deleteString(range.getStartOffset(), range.getEndOffset());

            return TextRange.from(range.getStartOffset(), 0);
        }
    }

    @Override
    public final boolean isApplicable(PsiElement[] elements) {
        return elements.length > 0;
    }

    @SuppressWarnings("unchecked")
    protected T surroundWithTemplate(Project project, PsiElement[] elements) {
        if (elements.length == 0) {
            throw new IllegalArgumentException("elements must not be empty");
        }
        PsiElement container = elements[0].getParent();
        if (container == null) {
            throw new IllegalArgumentException("elements must have a parent");
        }

        Pair<T, RsBlock> pair = createTemplate(project);
        T template = pair.first;
        RsBlock block = pair.second;
        RsSurroundWithUtils.addStatements(block, elements);
        template = (T) template.getClass().cast(
            container.addBefore(template, elements[0])
        );

        container.deleteChildRange(elements[0], elements[elements.length - 1]);
        return template;
    }
}
