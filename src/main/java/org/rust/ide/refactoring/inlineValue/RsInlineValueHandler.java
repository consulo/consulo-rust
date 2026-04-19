/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.refactoring.inlineValue;

import org.rust.lang.core.psi.ext.RsElementUtil;
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
import org.jetbrains.annotations.TestOnly;
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
    public void inlineElement(@NotNull Project project, @NotNull Editor editor, @NotNull PsiElement element) {
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
                com.intellij.openapi.wm.StatusBar statusBar = WindowManager.getInstance().getStatusBar(project);
                if (statusBar != null) {
                    statusBar.setInfo(RefactoringBundle.message("press.escape.to.remove.the.highlighting"));
                }
            }
        } else {
            RsInlineValueProcessor processor = getProcessor(project, context);
            processor.setPreviewUsages(false);
            processor.run();
        }
    }

    @Override
    public boolean canInlineElement(@NotNull PsiElement element) {
        return (element instanceof RsConstant && element.getNavigationElement() instanceof RsConstant) ||
            (element instanceof RsPatBinding && element.getNavigationElement() instanceof RsPatBinding);
    }

    @TestOnly
    public static void withMockInlineValueMode(@NotNull InlineValueMode mock, @NotNull Runnable action) {
        MOCK = mock;
        try {
            action.run();
        } finally {
            MOCK = null;
        }
    }

    @NotNull
    private static RsInlineValueProcessor getProcessor(@NotNull Project project, @NotNull InlineValueContext context) {
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
        @NotNull Project project,
        @NotNull Editor editor,
        @NotNull PsiElement element,
        @Nullable RsReference reference
    ) {
        InlineValueContext variableContext = getVariableDeclContext(project, editor, element, reference);
        if (variableContext != null) return variableContext;
        return getConstantContext(project, editor, element, reference);
    }

    @Nullable
    private static InlineValueContext getConstantContext(
        @NotNull Project project,
        @NotNull Editor editor,
        @NotNull PsiElement element,
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
        @NotNull Project project,
        @NotNull Editor editor,
        @NotNull PsiElement element,
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

    private static void showErrorHint(@NotNull Project project, @NotNull Editor editor, @NotNull String message) {
        CommonRefactoringUtil.showErrorHint(
            project, editor, message,
            RefactoringBundle.message("inline.variable.title"),
            "refactoring.inline"
        );
    }
}
