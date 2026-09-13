/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.fixes;

import org.rust.lang.core.psi.ext.impl.RsElementUtil;
import consulo.codeEditor.Editor;
import consulo.project.Project;
import consulo.language.psi.PsiElement;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.rust.RsBundle;
import org.rust.lang.core.psi.RsElementTypes;
import org.rust.lang.core.psi.RsPatStruct;
import org.rust.lang.core.psi.RsPatTupleStruct;
import org.rust.lang.core.psi.impl.RsPsiFactory;
import org.rust.lang.core.psi.ext.RsElement;
import consulo.localize.LocalizeValue;

public class AddPatRestFix extends RsQuickFixBase<PsiElement> {

    public AddPatRestFix(@Nonnull PsiElement element) {
        super(element);
    }

    @Nonnull
    @Override
    public consulo.localize.LocalizeValue getText() {
        return consulo.localize.LocalizeValue.of(RsBundle.message("intention.name.add"));
    }

    @Nonnull
        public consulo.localize.LocalizeValue getFamilyName() {
        return getText();
    }

    @Override
    public void invoke(@Nonnull Project project, @Nullable Editor editor, @Nonnull PsiElement element) {
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
