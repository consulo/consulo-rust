/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.intentions;

import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElement;
import org.rust.RsBundle;
import org.rust.ide.utils.PsiModificationUtil;
import org.rust.ide.utils.StructFieldsExpander;
import org.rust.lang.core.psi.RsElementTypes;
import org.rust.lang.core.psi.RsPsiFactory;
import org.rust.lang.core.psi.RsStructLiteral;
import org.rust.lang.core.psi.RsStructLiteralBody;
import org.rust.lang.core.psi.ext.RsPsiJavaUtil;

public class AddStructFieldsLiteralIntention extends RsElementBaseIntentionAction<AddStructFieldsLiteralIntention.Context> {

    @Override
    public String getText() {
        return RsBundle.message("intention.name.replace.with.actual.fields");
    }

    @Override
    public String getFamilyName() {
        return getText();
    }

    public static class Context {
        public final RsStructLiteral structLiteral;

        public Context(RsStructLiteral structLiteral) {
            this.structLiteral = structLiteral;
        }
    }

    @Override
    public Context findApplicableContext(Project project, Editor editor, PsiElement element) {
        boolean isAppropriate = RsPsiJavaUtil.elementType(element) == RsElementTypes.DOTDOT
            && element.getContext() != null
            && element.getContext().getContext() instanceof RsStructLiteral;
        if (!isAppropriate) return null;
        if (!PsiModificationUtil.canReplace(element)) return null;
        return new Context((RsStructLiteral) element.getContext().getParent());
    }

    @Override
    public void invoke(Project project, Editor editor, Context ctx) {
        RsStructLiteral structLiteral = ctx.structLiteral;
        removeDotsAndBaseStruct(structLiteral);
        StructFieldsExpander.addMissingFieldsToStructLiteral(new RsPsiFactory(project), editor, structLiteral, false);
    }

    protected void removeDotsAndBaseStruct(RsStructLiteral structLiteral) {
        RsStructLiteralBody structLiteralBody = structLiteral.getStructLiteralBody();
        PsiElement dotdot = structLiteralBody.getDotdot();
        if (dotdot != null) {
            PsiElement nextSibling = RsPsiJavaUtil.getNextNonCommentSibling(dotdot);
            if (nextSibling != null) nextSibling.delete();
            dotdot.delete();
        }
    }
}
