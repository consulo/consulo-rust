/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.refactoring.introduceParameter;

import consulo.dataContext.DataContext;
import consulo.codeEditor.Editor;
import consulo.project.Project;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiFile;
import consulo.language.editor.refactoring.action.RefactoringActionHandler;
import consulo.language.editor.refactoring.RefactoringBundle;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.rust.lang.core.psi.RsExpr;
import org.rust.lang.core.psi.RsFile;
import org.rust.lang.core.types.RsTypesUtil;
import org.rust.lang.core.types.ty.TyNever;
import org.rust.lang.core.types.ty.TyUnit;

import java.util.List;
import java.util.stream.Collectors;

import static org.rust.ide.refactoring.ExtraxtExpressionUtils.findCandidateExpressionsToExtract;
import static org.rust.ide.refactoring.ExtraxtExpressionUiUtils.*;

public class RsIntroduceParameterHandler implements RefactoringActionHandler {

    @Override
    public void invoke(@Nonnull Project project, @Nonnull Editor editor, @Nonnull PsiFile file, @Nullable DataContext dataContext) {
        if (!(file instanceof RsFile)) return;

        List<RsExpr> exprs = findCandidateExpressionsToExtract(editor, (RsFile) file)
            .stream()
            .filter(it -> !(RsTypesUtil.getType(it) instanceof TyUnit) && !(RsTypesUtil.getType(it) instanceof TyNever))
            .collect(Collectors.toList());

        switch (exprs.size()) {
            case 0: {
                String message = RefactoringBundle.message(editor.getSelectionModel().hasSelection()
                    ? "selected.block.should.represent.an.expression"
                    : "refactoring.introduce.selection.error"
                );
                showErrorMessageForExtractParameter(project, editor, message);
                break;
            }
            case 1:
                IntroduceParameterImpl.extractExpression(editor, exprs.get(0));
                break;
            default:
                showExpressionChooser(editor, exprs, expr ->
                    IntroduceParameterImpl.extractExpression(editor, expr)
                );
                break;
        }
    }

    @Override
    public void invoke(@Nonnull Project project, @Nonnull PsiElement [] elements, @Nullable DataContext dataContext) {
        // this doesn't get called from the editor.
    }
}
