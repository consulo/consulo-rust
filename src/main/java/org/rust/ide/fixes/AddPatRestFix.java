/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.fixes;

import org.rust.lang.core.psi.ext.RsElementUtil;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.rust.RsBundle;
import org.rust.lang.core.psi.RsElementTypes;
import org.rust.lang.core.psi.RsPatStruct;
import org.rust.lang.core.psi.RsPatTupleStruct;
import org.rust.lang.core.psi.RsPsiFactory;
import org.rust.lang.core.psi.ext.RsElement;

public class AddPatRestFix extends RsQuickFixBase<PsiElement> {

    public AddPatRestFix(@NotNull PsiElement element) {
        super(element);
    }

    @NotNull
    @Override
    public String getText() {
        return RsBundle.message("intention.name.add");
    }

    @NotNull
    @Override
    public String getFamilyName() {
        return getText();
    }

    @Override
    public void invoke(@NotNull Project project, @Nullable Editor editor, @NotNull PsiElement element) {
        PsiElement pat;
        PsiElement lBraceOrParen;
        PsiElement rBraceOrParen;

        if (element instanceof RsPatStruct) {
            RsPatStruct patStruct = (RsPatStruct) element;
            pat = patStruct;
            lBraceOrParen = patStruct.getLbrace();
            rBraceOrParen = patStruct.getRbrace();
        } else if (element instanceof RsPatTupleStruct) {
            RsPatTupleStruct patTuple = (RsPatTupleStruct) element;
            pat = patTuple;
            lBraceOrParen = patTuple.getLparen();
            rBraceOrParen = patTuple.getRparen();
        } else {
            return;
        }

        PsiElement lastSibling = RsElementUtil.getPrevNonCommentSibling(rBraceOrParen);
        if (lastSibling == null) return;
        RsPsiFactory psiFactory = new RsPsiFactory(project);

        PsiElement anchor;
        if (lastSibling.getNode().getElementType() == RsElementTypes.COMMA || lastSibling == lBraceOrParen) {
            anchor = lastSibling;
        } else {
            anchor = pat.addAfter(psiFactory.createComma(), lastSibling);
        }
        pat.addAfter(psiFactory.createPatRest(), anchor);
    }
}
