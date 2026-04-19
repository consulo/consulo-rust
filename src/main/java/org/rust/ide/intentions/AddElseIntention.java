/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.intentions;

import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElement;
import org.rust.RsBundle;
import org.rust.ide.intentions.util.macros.InvokeInside;
import org.rust.ide.utils.PsiInsertionPlace;
import org.rust.lang.core.psi.*;
import org.rust.lang.core.psi.ext.RsPsiJavaUtil;

public class AddElseIntention extends RsElementBaseIntentionAction<PsiInsertionPlace> {

    @Override
    public String getText() {
        return RsBundle.message("intention.name.add.else.branch.to.this.if.statement");
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
    public PsiInsertionPlace findApplicableContext(Project project, Editor editor, PsiElement element) {
        RsIfExpr ifExpr = RsPsiJavaUtil.ancestorStrict(element, RsIfExpr.class);
        if (ifExpr == null) return null;
        if (ifExpr.getElseBranch() != null) return null;
        RsBlock block = ifExpr.getBlock();
        if (block == null) return null;
        PsiElement rbrace = block.getRbrace();
        if (rbrace == null) return null;
        PsiElement lbrace = block.getLbrace();
        if (lbrace == null) return null;
        if (element.getTextRange().getStartOffset() >= lbrace.getTextRange().getEndOffset() && element != rbrace) return null;
        return PsiInsertionPlace.after(block);
    }

    @Override
    public void invoke(Project project, Editor editor, PsiInsertionPlace ctx) {
        RsIfExpr newIfExpr = (RsIfExpr) new RsPsiFactory(project).createExpression("if a {} else {}");
        PsiElement insertedElseBlock = ctx.insert(newIfExpr.getElseBranch());
        RsElseBranch elseBranch = ((RsIfExpr) insertedElseBlock.getParent()).getElseBranch();
        PsiElement elseBlock = elseBranch != null ? elseBranch.getBlock() : null;
        if (elseBlock == null) return;
        int elseBlockOffset = elseBlock.getTextOffset();
        org.rust.openapiext.Editor.moveCaretToOffset(editor, insertedElseBlock, elseBlockOffset + 1);
    }
}
