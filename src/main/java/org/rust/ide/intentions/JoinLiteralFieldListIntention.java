/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.intentions;

import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;
import org.rust.RsBundle;
import org.rust.lang.core.psi.RsElementTypes;
import org.rust.lang.core.psi.RsStructLiteralBody;
import org.rust.lang.core.psi.RsStructLiteralField;
import org.rust.lang.core.psi.ext.PsiElementExt;

import java.util.ArrayList;
import java.util.List;

public class JoinLiteralFieldListIntention extends JoinListIntentionBase<RsStructLiteralBody, RsStructLiteralField> {
    public JoinLiteralFieldListIntention() {
        super(RsStructLiteralBody.class, RsStructLiteralField.class,
            RsBundle.message("intention.name.put.fields.on.one.line"),
            " ", " ");
    }

    @NotNull
    @Override
    protected List<PsiElement> getElements(@NotNull RsStructLiteralBody context) {
        List<PsiElement> result = new ArrayList<>(super.getElements(context));
        PsiElement dotdot = context.getDotdot();
        if (dotdot != null) {
            result.add(dotdot);
        }
        return result;
    }

    @NotNull
    @Override
    protected PsiElement getEndElement(@NotNull RsStructLiteralBody ctx, @NotNull PsiElement element) {
        if (PsiElementExt.getElementType(element) == RsElementTypes.DOTDOT) {
            PsiElement expr = ctx.getExpr();
            if (expr != null) {
                return getEndElement(ctx, expr);
            }
            return element;
        }
        return super.getEndElement(ctx, element);
    }
}
