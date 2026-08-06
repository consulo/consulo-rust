/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.refactoring.introduceVariable;

import consulo.dataContext.DataContext;
import consulo.codeEditor.Editor;
import consulo.project.Project;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiFile;
import consulo.language.editor.refactoring.action.RefactoringActionHandler;
import consulo.language.editor.refactoring.RefactoringBundle;
import consulo.language.editor.refactoring.util.CommonRefactoringUtil;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.rust.RsBundle;
import org.rust.lang.core.psi.RsExpr;
import org.rust.lang.core.psi.RsFile;
import org.rust.openapiext.Testmark;

import java.util.List;
import java.util.function.Consumer;

import static org.rust.ide.refactoring.ExtraxtExpressionUtils.findCandidateExpressionsToExtract;
import static org.rust.ide.refactoring.ExtraxtExpressionUiUtils.showExpressionChooser;
import org.rust.ide.refactoring.introduceVariable.IntroduceVariableImpl;

public class RsIntroduceVariableHandler implements RefactoringActionHandler {

    @Override
    public void invoke(@Nonnull Project project, @Nonnull Editor editor, @Nonnull PsiFile file, @Nullable DataContext dataContext) {
        if (!(file instanceof RsFile)) return;
        List<RsExpr> exprs = findCandidateExpressionsToExtract(editor, (RsFile) file);

        if (exprs.isEmpty()) {
            String message = RefactoringBundle.message(
                editor.getSelectionModel().hasSelection()
                    ? "selected.block.should.represent.an.expression"
                    : "refactoring.introduce.selection.error"
            );
            String title = RefactoringBundle.message("introduce.variable.title");
            String helpId = "refactoring.extractVariable";
            CommonRefactoringUtil.showErrorHint(project, editor, message, title, helpId);
        } else {
            Consumer<RsExpr> extractor = expr ->
                IntroduceVariableImpl.extractExpression(
                    editor, expr, false, RsBundle.message("command.name.introduce.local.variable")
                );
            if (exprs.size() == 1) {
                extractor.accept(exprs.get(0));
            } else {
                showExpressionChooser(editor, exprs, extractor::accept);
            }
        }
    }

    @Override
    public void invoke(@Nonnull Project project, @Nonnull PsiElement [] elements, @Nullable DataContext dataContext) {
        // this doesn't get called from the editor.
    }
}
