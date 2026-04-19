/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.refactoring.inlineFunction;

import com.intellij.codeInsight.TargetElementUtil;
import com.intellij.lang.Language;
import com.intellij.lang.refactoring.InlineActionHandler;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.WindowManager;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiReference;
import com.intellij.refactoring.RefactoringBundle;
import com.intellij.refactoring.util.CommonRefactoringUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.rust.RsBundle;
import org.rust.lang.RsLanguage;
import org.rust.lang.core.psi.RsFunction;
import org.rust.lang.core.psi.ext.RsFunctionUtil;
import org.rust.lang.core.resolve.ref.RsReference;
import org.rust.openapiext.OpenApiUtil;

public class RsInlineFunctionHandler extends InlineActionHandler {
    private static final String HELP_ID = "refactoring.inlineMethod";

    @Override
    public boolean isEnabledOnElement(@NotNull PsiElement element) {
        return canInlineElement(element);
    }

    @Override
    public void inlineElement(@NotNull Project project, @NotNull Editor editor, @NotNull PsiElement element) {
        RsFunction function = (RsFunction) element;

        PsiReference reference = TargetElementUtil.findReference(editor, editor.getCaretModel().getOffset());

        if (RsInlineFunctionProcessor.doesFunctionHaveMultipleReturns(function)) {
            errorHint(project, editor, RsBundle.message("dialog.message.cannot.inline.function.with.more.than.one.exit.points"));
            return;
        }

        boolean allowInlineThisOnly = false;
        if (RsInlineFunctionProcessor.isFunctionRecursive(function)) {
            if (reference != null) {
                allowInlineThisOnly = true;
            } else {
                errorHint(project, editor, RsBundle.message("dialog.message.cannot.inline.function.with.recursive.calls"));
                return;
            }
        }

        if (reference != null && RsInlineFunctionProcessor.checkIfLoopCondition(function, reference.getElement())) {
            errorHint(project, editor, RsBundle.message("dialog.message.cannot.inline.multiline.function.into.while.loop.condition"));
            return;
        }

        if (RsFunctionUtil.getBlock(function) == null) {
            errorHint(project, editor, RsBundle.message("dialog.message.cannot.inline.empty.function"));
            return;
        }

        if (!org.rust.openapiext.OpenApiUtil.isUnitTestMode()) {
            RsInlineFunctionDialog dialog = new RsInlineFunctionDialog(function, (RsReference) reference, allowInlineThisOnly);
            dialog.show();
            if (!dialog.isOK()) {
                com.intellij.openapi.wm.StatusBar statusBar = WindowManager.getInstance().getStatusBar(function.getProject());
                if (statusBar != null) {
                    statusBar.setInfo(RefactoringBundle.message("press.escape.to.remove.the.highlighting"));
                }
            }
        } else {
            RsInlineFunctionProcessor processor = new RsInlineFunctionProcessor(
                project, function, (RsReference) reference, false, true
            );
            processor.run();
        }
    }

    @Override
    public boolean isEnabledForLanguage(@Nullable Language l) {
        return l == RsLanguage.INSTANCE;
    }

    @Override
    public boolean canInlineElementInEditor(@NotNull PsiElement element, @Nullable Editor editor) {
        return canInlineElement(element);
    }

    @Override
    public boolean canInlineElement(@NotNull PsiElement element) {
        return element instanceof RsFunction && element.getNavigationElement() instanceof RsFunction;
    }

    private void errorHint(@NotNull Project project, @NotNull Editor editor, @NotNull String message) {
        CommonRefactoringUtil.showErrorHint(
            project, editor, message,
            RefactoringBundle.message("inline.method.title"),
            HELP_ID
        );
    }
}
