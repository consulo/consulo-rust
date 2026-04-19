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
import org.rust.lang.core.psi.*;
import org.rust.lang.core.psi.ext.RsPsiJavaUtil;

public class AddStructFieldsPatIntention extends RsElementBaseIntentionAction<AddStructFieldsPatIntention.Context> {

    @Override
    public String getText() {
        return RsBundle.message("intention.name.replace.with.actual.fields");
    }

    @Override
    public String getFamilyName() {
        return getText();
    }

    public static class Context {
        public final RsPat structPat;

        public Context(RsPat structPat) {
            this.structPat = structPat;
        }
    }

    @Override
    public Context findApplicableContext(Project project, Editor editor, PsiElement element) {
        if (RsPsiJavaUtil.elementType(element) != RsElementTypes.DOTDOT) return null;
        PsiElement context = element.getContext();
        if (!(context instanceof RsPatRest)) return null;
        PsiElement pat = context.getContext();
        if (!(pat instanceof RsPatStruct) && !(pat instanceof RsPatTupleStruct)) return null;
        if (!PsiModificationUtil.canReplace(pat)) return null;
        return new Context((RsPat) pat);
    }

    @Override
    public void invoke(Project project, Editor editor, Context ctx) {
        RsPsiFactory factory = new RsPsiFactory(project);
        RsPat structPat = ctx.structPat;
        if (structPat instanceof RsPatStruct) {
            StructFieldsExpander.expandStructFields(factory, (RsPatStruct) structPat);
        } else if (structPat instanceof RsPatTupleStruct) {
            StructFieldsExpander.expandTupleStructFields(factory, editor, (RsPatTupleStruct) structPat);
        }
    }
}
