/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.refactoring;

import consulo.language.Language;
import consulo.dataContext.DataContext;
import consulo.codeEditor.Editor;
import consulo.project.Project;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiFile;
import consulo.language.editor.refactoring.action.RefactoringActionHandler;
import consulo.language.editor.refactoring.action.BaseRefactoringAction;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.rust.lang.RsLanguage;

public abstract class RsBaseEditorRefactoringAction extends BaseRefactoringAction {

    @Override
    protected boolean isAvailableInEditorOnly() {
        return true;
    }

    @Override
    public abstract boolean isAvailableOnElementInEditorAndFile(
        @Nonnull PsiElement element,
        @Nonnull Editor editor,
        @Nonnull PsiFile file,
        @Nonnull DataContext context
    );

    @Override
    protected boolean isEnabledOnElements(@Nonnull PsiElement[] elements) {
        return false;
    }

    @Override
    @Nonnull
    public RefactoringActionHandler getHandler(@Nonnull DataContext dataContext) {
        return new Handler();
    }

    @Override
    protected boolean isAvailableForLanguage(@Nonnull Language language) {
        return language.is(RsLanguage.INSTANCE);
    }

    public abstract void invoke(@Nonnull Project project, @Nonnull Editor editor, @Nonnull PsiFile file, @Nullable DataContext dataContext);

    private class Handler implements RefactoringActionHandler {
        @Override
        public void invoke(@Nonnull Project project, @Nonnull Editor editor, @Nonnull PsiFile file, @Nullable DataContext dataContext) {
            RsBaseEditorRefactoringAction.this.invoke(project, editor, file, dataContext);
        }

        @Override
        public void invoke(@Nonnull Project project, @Nonnull PsiElement[] elements, @Nullable DataContext dataContext) {
            // never called from editor
        }
    }
}
