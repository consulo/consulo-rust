/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.intentions;

import com.intellij.psi.PsiElement;
import org.rust.RsBundle;
import org.rust.lang.core.psi.RsElementTypes;
import org.rust.lang.core.psi.RsStructLiteralBody;
import org.rust.lang.core.psi.RsStructLiteralField;
import org.rust.lang.core.psi.ext.RsPsiJavaUtil;

import java.util.ArrayList;
import java.util.List;

/**
 * This intention has a special case for the dotdot (..) element.
 */
public class ChopLiteralFieldListIntention extends ChopListIntentionBase<RsStructLiteralBody, RsStructLiteralField> {
    public ChopLiteralFieldListIntention() {
        super(RsStructLiteralBody.class, RsStructLiteralField.class,
            RsBundle.message("intention.name.put.fields.on.separate.lines"));
    }

    @Override
    public List<PsiElement> getElements(RsStructLiteralBody context) {
        List<PsiElement> result = new ArrayList<>(super.getElements(context));
        PsiElement dotdot = context.getDotdot();
        if (dotdot != null) {
            result.add(dotdot);
        }
        return result;
    }

    @Override
    public PsiElement getEndElement(RsStructLiteralBody ctx, PsiElement element) {
        if (RsPsiJavaUtil.elementType(element) == RsElementTypes.DOTDOT) {
            PsiElement expr = ctx.getExpr();
            if (expr != null) {
                return getEndElement(ctx, expr);
            }
            return element;
        }
        return super.getEndElement(ctx, element);
    }
}
