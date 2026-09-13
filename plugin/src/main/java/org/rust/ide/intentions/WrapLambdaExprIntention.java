/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.intentions;

import consulo.codeEditor.Editor;
import consulo.project.Project;
import consulo.language.psi.PsiElement;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.rust.RsBundle;
import org.rust.ide.intentions.util.macros.InvokeInside;
import org.rust.ide.utils.PsiModificationUtil;
import org.rust.lang.core.psi.RsBlockExpr;
import org.rust.lang.core.psi.RsExpr;
import org.rust.lang.core.psi.RsLambdaExpr;
import org.rust.lang.core.psi.impl.RsPsiFactory;
import org.rust.lang.core.psi.ext.impl.PsiElementExt;
import org.rust.lang.core.psi.ext.impl.RsBlockUtil;
import org.rust.openapiext.ui.EditorExt;
import org.rust.lang.core.psi.ext.impl.RsFunctionUtil;
import consulo.localize.LocalizeValue;

public class WrapLambdaExprIntention extends RsElementBaseIntentionAction<RsExpr> {
    @Nonnull
    @Override
    public consulo.localize.LocalizeValue getText() {
        return consulo.localize.LocalizeValue.of(RsBundle.message("intention.name.add.braces.to.lambda.expression"));
        }

    @Nonnull
    public consulo.localize.LocalizeValue getFamilyName() {
        return getText();
        }

    @Nonnull
    @Override
    public InvokeInside getAttributeMacroHandlingStrategy() {
        return InvokeInside.MACRO_CALL;
    }

    @Nullable
    @Override
    public RsExpr findApplicableContext(@Nonnull Project project, @Nonnull Editor editor, @Nonnull PsiElement element) {
        RsLambdaExpr lambdaExpr = PsiElementExt.ancestorStrict(element, RsLambdaExpr.class);
        if (lambdaExpr == null) return null;
        RsExpr body = lambdaExpr.getExpr();
        if (body == null) return null;
        if (body instanceof RsBlockExpr) return null;
        if (!PsiModificationUtil.canReplace(body)) return null;
        return body;
    }

    @Override
    public void invoke(@Nonnull Project project, @Nonnull Editor editor, @Nonnull RsExpr ctx) {
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
