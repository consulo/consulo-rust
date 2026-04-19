/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.refactoring.changeSignature;

import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.util.NlsContexts.DialogMessage;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.refactoring.RefactoringBundle;
import com.intellij.refactoring.changeSignature.ChangeSignatureHandler;
import com.intellij.refactoring.util.CommonRefactoringUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.rust.RsBundle;
import org.rust.cargo.project.workspace.PackageOrigin;
import org.rust.lang.core.psi.*;
import org.rust.lang.core.psi.ext.*;
import org.rust.openapiext.DataContextExtUtil;
import org.rust.openapiext.OpenApiUtil;

public class RsChangeSignatureHandler implements ChangeSignatureHandler {

    @NotNull
    @Override
    public String getTargetNotFoundMessage() {
        return RsBundle.message("dialog.message.caret.should.be.positioned.at.function.or.method");
    }

    @Nullable
    @Override
    public RsFunction findTargetMember(@NotNull PsiElement element) {
        PsiElement el = element;
        while (el != null) {
            if (el instanceof RsFunction) {
                return (RsFunction) el;
            }
            if (el instanceof RsCallExpr) {
                RsExpr expr = ((RsCallExpr) el).getExpr();
                if (expr instanceof RsPathExpr) {
                    PsiElement resolved = ((RsPathExpr) expr).getPath().getReference() != null
                        ? ((RsPathExpr) expr).getPath().getReference().resolve()
                        : null;
                    if (resolved instanceof RsFunction) return (RsFunction) resolved;
                }
                return null;
            }
            if (el instanceof RsMethodCall) {
                PsiElement resolved = ((RsMethodCall) el).getReference().resolve();
                return resolved instanceof RsFunction ? (RsFunction) resolved : null;
            }
            if (el instanceof RsBlock) {
                return null;
            }
            el = el.getParent();
        }
        return null;
    }

    @Override
    public void invoke(@NotNull Project project, @NotNull PsiElement[] elements, @Nullable DataContext dataContext) {
        if (elements.length != 1) return;
        if (!(elements[0] instanceof RsFunction)) return;
        RsFunction function = (RsFunction) elements[0];
        Editor editor = dataContext != null ? DataContextExtUtil.getEditor(dataContext) : null;
        invokeOnFunction(function, editor);
    }

    @Override
    public void invoke(@NotNull Project project, @Nullable Editor editor, @Nullable PsiFile file, @Nullable DataContext dataContext) {
        PsiElement element = dataContext != null ? DataContextExtUtil.getElementUnderCaretInEditor(dataContext) : null;
        if (!(element instanceof RsFunction)) return;
        invokeOnFunction((RsFunction) element, editor);
    }

    private void invokeOnFunction(@NotNull RsFunction function, @Nullable Editor editor) {
        RsFunction targetFunction = getSuperMethod(function);
        if (targetFunction == null) {
            targetFunction = function;
        }
        showRefactoringDialog(targetFunction, editor);
    }

    private void showRefactoringDialog(@NotNull RsFunction function, @Nullable Editor editor) {
        RsChangeFunctionSignatureConfig config = RsChangeFunctionSignatureConfig.create(function);
        Project project = function.getProject();

        String error = checkFunction(function);
        if (error == null) {
            RsChangeSignatureDialog.showChangeFunctionSignatureDialog(project, config);
        } else if (editor != null) {
            showCannotRefactorErrorHint(project, editor, error);
        }
    }

    private void showCannotRefactorErrorHint(
        @NotNull Project project,
        @NotNull Editor editor,
        @NotNull @DialogMessage String message
    ) {
        CommonRefactoringUtil.showErrorHint(project, editor,
            RefactoringBundle.getCannotRefactorMessage(message),
            RefactoringBundle.message("changeSignature.refactoring.name"),
            "refactoring.changeSignature"
        );
    }

    public static boolean isChangeSignatureAvailable(@NotNull RsFunction function) {
        return checkFunction(function) == null;
    }

    @SuppressWarnings("UnstableApiUsage")
    @DialogMessage
    @Nullable
    private static String checkFunction(@NotNull RsFunction function) {
        if (function.getContainingCrate().getOrigin() != PackageOrigin.WORKSPACE) {
            return RsBundle.message("dialog.message.cannot.change.signature.function.in.foreign.crate");
        }
        if (!function.getValueParameters().equals(function.getRawValueParameters())) {
            return RsBundle.message("refactoring.change.signature.error.cfg.disabled.parameters");
        }
        return null;
    }

    @Nullable
    private static RsFunction getSuperMethod(@NotNull RsFunction function) {
        RsAbstractable superItem = function.getSuperItem();
        if (!(superItem instanceof RsFunction)) return null;
        RsFunction superMethod = (RsFunction) superItem;
        String functionName = function.getName();
        if (functionName == null) return null;
        RsAbstractableOwner owner = RsAbstractableUtil.getOwner(superMethod);
        if (!(owner instanceof RsAbstractableOwner.Trait)) return null;
        String traitName = ((RsAbstractableOwner.Trait) owner).getTrait().getName();
        if (traitName == null) return null;

        String message = RsBundle.message("refactoring.change.signature.refactor.super.function",
            functionName, traitName);
        int choice;
        if (org.rust.openapiext.OpenApiUtil.isUnitTestMode()) {
            choice = Messages.YES;
        } else {
            choice = Messages.showYesNoCancelDialog(function.getProject(), message,
                RsBundle.message("refactoring.change.signature.name"),
                Messages.getQuestionIcon());
        }
        if (choice == Messages.YES) {
            return superMethod;
        }
        return null;
    }
}
