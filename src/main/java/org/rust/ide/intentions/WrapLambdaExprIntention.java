/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.intentions;

import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.rust.RsBundle;
import org.rust.ide.intentions.util.macros.InvokeInside;
import org.rust.ide.utils.PsiModificationUtil;
import org.rust.lang.core.psi.RsBlockExpr;
import org.rust.lang.core.psi.RsExpr;
import org.rust.lang.core.psi.RsLambdaExpr;
import org.rust.lang.core.psi.RsPsiFactory;
import org.rust.lang.core.psi.ext.PsiElementExt;
import org.rust.lang.core.psi.ext.RsBlockUtil;
import org.rust.openapiext.EditorExt;
import org.rust.lang.core.psi.ext.RsFunctionUtil;

public class WrapLambdaExprIntention extends RsElementBaseIntentionAction<RsExpr> {
    @NotNull
    @Override
    public String getText() {
        return RsBundle.message("intention.name.add.braces.to.lambda.expression");
    }

    @NotNull
    @Override
    public String getFamilyName() {
        return getText();
    }

    @NotNull
    @Override
    public InvokeInside getAttributeMacroHandlingStrategy() {
        return InvokeInside.MACRO_CALL;
    }

    @Nullable
    @Override
    public RsExpr findApplicableContext(@NotNull Project project, @NotNull Editor editor, @NotNull PsiElement element) {
        RsLambdaExpr lambdaExpr = PsiElementExt.ancestorStrict(element, RsLambdaExpr.class);
        if (lambdaExpr == null) return null;
        RsExpr body = lambdaExpr.getExpr();
        if (body == null) return null;
        if (body instanceof RsBlockExpr) return null;
        if (!PsiModificationUtil.canReplace(body)) return null;
        return body;
    }

    @Override
    public void invoke(@NotNull Project project, @NotNull Editor editor, @NotNull RsExpr ctx) {
        int relativeCaretPosition = editor.getCaretModel().getOffset() - ctx.getTextOffset();

        String bodyStr = "\n" + ctx.getText() + "\n";
        RsBlockExpr blockExpr = new RsPsiFactory(project).createBlockExpr(bodyStr);

        RsBlockExpr insertedBlock = (RsBlockExpr) ctx.replace(blockExpr);
        PsiElement tailStmt = RsBlockUtil.getSyntaxTailStmt(insertedBlock.getBlock());
        if (tailStmt == null) return;
        int offset = tailStmt.getTextOffset();
        EditorExt.moveCaretToOffset(editor, insertedBlock, offset + relativeCaretPosition);
    }
}
