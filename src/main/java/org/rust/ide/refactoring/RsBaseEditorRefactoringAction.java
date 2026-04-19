/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.refactoring;

import com.intellij.lang.Language;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.refactoring.RefactoringActionHandler;
import com.intellij.refactoring.actions.BaseRefactoringAction;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.rust.lang.RsLanguage;

public abstract class RsBaseEditorRefactoringAction extends BaseRefactoringAction {

    @Override
    protected boolean isAvailableInEditorOnly() {
        return true;
    }

    @Override
    public abstract boolean isAvailableOnElementInEditorAndFile(
        @NotNull PsiElement element,
        @NotNull Editor editor,
        @NotNull PsiFile file,
        @NotNull DataContext context
    );

    @Override
    protected boolean isEnabledOnElements(@NotNull PsiElement[] elements) {
        return false;
    }

    @Override
    @NotNull
    public RefactoringActionHandler getHandler(@NotNull DataContext dataContext) {
        return new Handler();
    }

    @Override
    protected boolean isAvailableForLanguage(@NotNull Language language) {
        return language.is(RsLanguage.INSTANCE);
    }

    public abstract void invoke(@NotNull Project project, @NotNull Editor editor, @NotNull PsiFile file, @Nullable DataContext dataContext);

    private class Handler implements RefactoringActionHandler {
        @Override
        public void invoke(@NotNull Project project, @NotNull Editor editor, @NotNull PsiFile file, @Nullable DataContext dataContext) {
            RsBaseEditorRefactoringAction.this.invoke(project, editor, file, dataContext);
        }

        @Override
        public void invoke(@NotNull Project project, @NotNull PsiElement[] elements, @Nullable DataContext dataContext) {
            // never called from editor
        }
    }
}
