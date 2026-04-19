/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.refactoring.generate;

import com.intellij.codeInsight.CodeInsightActionHandler;
import com.intellij.codeInsight.actions.CodeInsightAction;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiFile;
import org.jetbrains.annotations.NotNull;

public abstract class BaseGenerateAction extends CodeInsightAction {
    @NotNull
    protected abstract BaseGenerateHandler getGenerateHandler();

    @NotNull
    @Override
    protected CodeInsightActionHandler getHandler() {
        return getGenerateHandler();
    }

    @Override
    protected boolean isValidForFile(@NotNull Project project, @NotNull Editor editor, @NotNull PsiFile file) {
        return getGenerateHandler().isValidFor(editor, file);
    }
}
