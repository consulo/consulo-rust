/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.refactoring.inlineValue;

import org.rust.lang.core.psi.ext.RsElementUtil;
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
import org.rust.lang.core.psi.*;
import org.rust.lang.core.psi.ext.RsElement;
import org.rust.lang.core.resolve.ref.RsReference;
import org.rust.openapiext.OpenApiUtil;

public class RsInlineValueHandler extends InlineActionHandler {
    @Nullable
    private static InlineValueMode MOCK = null;

    @Override
    public boolean isEnabledForLanguage(@Nullable Language language) {
        return language instanceof RsLanguage;
    }

    @Override
    public void inlineElement(@Nonnull Project project, @Nonnull Editor editor, @Nonnull PsiElement element) {
        PsiReference ref = TargetElementUtil.findReference(editor, editor.getCaretModel().getOffset());
        RsReference reference = ref instanceof RsReference ? (RsReference) ref : null;

        if (reference != null && reference.getElement() == element) {
            reference = null;
        }
        InlineValueContext context = getContext(project, editor, element, reference);
        if (context == null) return;

        if (!org.rust.openapiext.OpenApiUtil.isUnitTestMode()) {
            RsInlineValueDialog dialog = new RsInlineValueDialog(context);
            dialog.show();
            if (!dialog.isOK()) {
                // status bar "setInfo" API not available in Consulo
            }
        } else {
            RsInlineValueProcessor processor = getProcessor(project, context);
            processor.setPreviewUsages(false);
            processor.run();
        }
    }

    @Override
    public boolean canInlineElement(@Nonnull PsiElement element) {
        return (element instanceof RsConstant && element.getNavigationElement() instanceof RsConstant) ||
            (element instanceof RsPatBinding && element.getNavigationElement() instanceof RsPatBinding);
    }

    
    public static void withMockInlineValueMode(@Nonnull InlineValueMode mock, @Nonnull Runnable action) {
        MOCK = mock;
        try {
            action.run();
        } finally {
            MOCK = null;
        }
    }

    @Nonnull
    private static RsInlineValueProcessor getProcessor(@Nonnull Project project, @Nonnull InlineValueContext context) {
        InlineValueMode mode;
        if (org.rust.openapiext.OpenApiUtil.isUnitTestMode() && MOCK != null) {
            mode = MOCK;
        } else {
            mode = InlineValueMode.INLINE_ALL_AND_REMOVE_ORIGINAL;
        }
        return new RsInlineValueProcessor(project, context, mode);
    }

    @Nullable
    private static InlineValueContext getContext(
        @Nonnull Project project,
        @Nonnull Editor editor,
        @Nonnull PsiElement element,
        @Nullable RsReference reference
    ) {
        InlineValueContext variableContext = getVariableDeclContext(project, editor, element, reference);
        if (variableContext != null) return variableContext;
        return getConstantContext(project, editor, element, reference);
    }

    @Nullable
    private static InlineValueContext getConstantContext(
        @Nonnull Project project,
        @Nonnull Editor editor,
        @Nonnull PsiElement element,
        @Nullable RsReference reference
    ) {
        if (!(element instanceof RsConstant)) return null;
        RsConstant constant = (RsConstant) element;
        RsExpr expr = constant.getExpr();
        if (expr == null) {
            showErrorHint(project, editor, RsBundle.message("dialog.message.cannot.inline.constant.without.expression"));
            return null;
        }
        return new InlineValueContext.Constant(constant, expr, reference);
    }

    @Nullable
    private static InlineValueContext getVariableDeclContext(
        @Nonnull Project project,
        @Nonnull Editor editor,
        @Nonnull PsiElement element,
        @Nullable RsReference reference
    ) {
        if (!(element instanceof RsPatBinding)) return null;
        RsPatBinding binding = (RsPatBinding) element;
        RsLetDecl decl = RsElementUtil.ancestorOrSelf(binding, RsLetDecl.class);
        if (decl == null || decl.getExpr() == null) {
            showErrorHint(project, editor, RsBundle.message("dialog.message.cannot.inline.variable.without.expression"));
            return null;
        }
        if (!(decl.getPat() instanceof RsPatIdent)) {
            showErrorHint(project, editor, RsBundle.message("dialog.message.cannot.inline.variable.without.identifier"));
            return null;
        }
        return new InlineValueContext.Variable(binding, decl, decl.getExpr(), reference);
    }

    private static void showErrorHint(@Nonnull Project project, @Nonnull Editor editor, @Nonnull String message) {
        CommonRefactoringUtil.showErrorHint(
            project, editor, message,
            RefactoringBundle.message("inline.variable.title"),
            "refactoring.inline"
        );
    }
}
