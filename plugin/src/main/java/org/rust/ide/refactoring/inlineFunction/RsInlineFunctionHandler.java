/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.refactoring.inlineFunction;

import consulo.language.editor.TargetElementUtil;
import consulo.language.Language;
import consulo.language.editor.refactoring.inline.InlineActionHandler;
import consulo.codeEditor.Editor;
import consulo.project.Project;
import consulo.project.ui.wm.WindowManager;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiReference;
import consulo.language.editor.refactoring.RefactoringBundle;
import consulo.language.editor.refactoring.util.CommonRefactoringUtil;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.rust.RsBundle;
import org.rust.lang.RsLanguage;
import org.rust.lang.core.psi.RsFunction;
import org.rust.lang.core.psi.ext.RsFunctionUtil;
import org.rust.lang.core.resolve.ref.RsReference;
import org.rust.openapiext.OpenApiUtil;
import consulo.annotation.component.ExtensionImpl;

@ExtensionImpl
public class RsInlineFunctionHandler extends InlineActionHandler {
    private static final String HELP_ID = "refactoring.inlineMethod";

    @Override
    public boolean isEnabledOnElement(@Nonnull PsiElement element) {
        return canInlineElement(element);
    }

    @Override
    public void inlineElement(@Nonnull Project project, @Nonnull Editor editor, @Nonnull PsiElement element) {
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
                // status bar "setInfo" API not available in Consulo
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
    public boolean canInlineElementInEditor(@Nonnull PsiElement element, @Nullable Editor editor) {
        return canInlineElement(element);
    }

    @Override
    public boolean canInlineElement(@Nonnull PsiElement element) {
        return element instanceof RsFunction && element.getNavigationElement() instanceof RsFunction;
    }

    private void errorHint(@Nonnull Project project, @Nonnull Editor editor, @Nonnull String message) {
        CommonRefactoringUtil.showErrorHint(
            project, editor, message,
            RefactoringBundle.message("inline.method.title"),
            HELP_ID
        );
    }
}
