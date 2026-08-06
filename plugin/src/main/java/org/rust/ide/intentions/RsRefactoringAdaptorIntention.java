/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.intentions;

import consulo.language.editor.intention.BaseElementAtCaretIntentionAction;
import consulo.dataContext.DataContext;
import consulo.codeEditor.Editor;
import consulo.project.Project;
import consulo.language.psi.PsiElement;
import jakarta.annotation.Nonnull;
import org.rust.ide.refactoring.RsBaseEditorRefactoringAction;

public abstract class RsRefactoringAdaptorIntention extends BaseElementAtCaretIntentionAction {

    @Nonnull
    public abstract RsBaseEditorRefactoringAction getRefactoringAction();

    // refactorings start its own write action
    @Override
    public boolean startInWriteAction() {
        return false;
    }

    @Override
    public boolean isAvailable(@Nonnull Project project, Editor editor, @Nonnull PsiElement element) {
        return getRefactoringAction().isAvailableOnElementInEditorAndFile(element, editor, element.getContainingFile(), DataContext.EMPTY_CONTEXT);
    }

    @Override
    public void invoke(@Nonnull Project project, Editor editor, @Nonnull PsiElement element) {
        getRefactoringAction()
            .getHandler(DataContext.EMPTY_CONTEXT)
            .invoke(project, editor, element.getContainingFile(), DataContext.EMPTY_CONTEXT);
    }
}
