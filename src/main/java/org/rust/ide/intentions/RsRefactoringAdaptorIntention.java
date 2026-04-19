/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.intentions;

import com.intellij.codeInsight.intention.BaseElementAtCaretIntentionAction;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;
import org.rust.ide.refactoring.RsBaseEditorRefactoringAction;

public abstract class RsRefactoringAdaptorIntention extends BaseElementAtCaretIntentionAction {

    @NotNull
    public abstract RsBaseEditorRefactoringAction getRefactoringAction();

    // refactorings start its own write action
    @Override
    public boolean startInWriteAction() {
        return false;
    }

    @Override
    public boolean isAvailable(@NotNull Project project, Editor editor, @NotNull PsiElement element) {
        return getRefactoringAction().isAvailableOnElementInEditorAndFile(element, editor, element.getContainingFile(), DataContext.EMPTY_CONTEXT);
    }

    @Override
    public void invoke(@NotNull Project project, Editor editor, @NotNull PsiElement element) {
        getRefactoringAction()
            .getHandler(DataContext.EMPTY_CONTEXT)
            .invoke(project, editor, element.getContainingFile(), DataContext.EMPTY_CONTEXT);
    }
}
