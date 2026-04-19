/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.intentions;

import com.intellij.codeInsight.intention.preview.IntentionPreviewInfo;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import org.rust.RsBundle;
import org.rust.ide.intentions.util.macros.InvokeInside;
import org.rust.ide.refactoring.introduceVariable.IntroduceVariableImpl;
import org.rust.ide.utils.PsiModificationUtil;
import org.rust.lang.core.psi.*;
import org.rust.lang.core.types.RsTypesUtil;
import org.rust.lang.core.types.ty.TyUnit;

public class IntroduceLocalVariableIntention extends RsElementBaseIntentionAction<RsExpr> {

    @Override
    public String getText() {
        return RsBundle.message("intention.name.introduce.local.variable");
    }

    @Override
    public String getFamilyName() {
        return getText();
    }

    @Override
    public InvokeInside getAttributeMacroHandlingStrategy() {
        return InvokeInside.MACRO_CALL;
    }

    @Override
    public RsExpr findApplicableContext(Project project, Editor editor, PsiElement element) {
        PsiElement current = element;
        while (current != null && !(current instanceof RsBlock) && !(current instanceof RsMatchExpr) && !(current instanceof RsLambdaExpr)) {
            PsiElement parent = current.getParent();
            if (parent instanceof RsRetExpr
                || parent instanceof RsExprStmt
                || (parent instanceof RsMatchArm && current == ((RsMatchArm) parent).getExpr())
                || (parent instanceof RsLambdaExpr && current == ((RsLambdaExpr) parent).getExpr())) {
                if (current instanceof RsExpr) {
                    RsExpr expr = (RsExpr) current;
                    if (RsTypesUtil.getType(expr) instanceof TyUnit) return null;
                    if (!PsiModificationUtil.canReplace(expr)) return null;
                    return expr;
                }
            }
            current = current.getParent();
        }
        return null;
    }

    @Override
    public void invoke(Project project, Editor editor, RsExpr ctx) {
        IntroduceVariableImpl.extractExpression(
            editor, ctx, false, RsBundle.message("command.name.introduce.local.variable")
        );
    }

    @Override
    public IntentionPreviewInfo generatePreview(Project project, Editor editor, PsiFile file) {
        return IntentionPreviewInfo.EMPTY;
    }
}
