/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.refactoring.generate;

import consulo.language.editor.action.CodeInsightActionHandler;
import consulo.language.editor.action.CodeInsightAction;
import consulo.codeEditor.Editor;
import consulo.project.Project;
import consulo.language.psi.PsiFile;
import jakarta.annotation.Nonnull;

public abstract class BaseGenerateAction extends CodeInsightAction {
    @Nonnull
    protected abstract BaseGenerateHandler getGenerateHandler();

    @Nonnull
    @Override
    protected CodeInsightActionHandler getHandler() {
        return getGenerateHandler();
    }

    @Override
    protected boolean isValidForFile(@Nonnull Project project, @Nonnull Editor editor, @Nonnull PsiFile file) {
        return getGenerateHandler().isValidFor(editor, file);
    }
}
