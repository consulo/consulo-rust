/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.intentions;

import consulo.language.psi.PsiElement;
import jakarta.annotation.Nonnull;
import org.rust.RsBundle;
import org.rust.lang.core.psi.RsElementTypes;
import org.rust.lang.core.psi.RsStructLiteralBody;
import org.rust.lang.core.psi.RsStructLiteralField;
import org.rust.lang.core.psi.ext.impl.PsiElementExt;

import java.util.ArrayList;
import java.util.List;

public class JoinLiteralFieldListIntention extends JoinListIntentionBase<RsStructLiteralBody, RsStructLiteralField> {
    public JoinLiteralFieldListIntention() {
        super(RsStructLiteralBody.class, RsStructLiteralField.class,
            RsBundle.message("intention.name.put.fields.on.one.line"),
            " ", " ");
    }

    @Nonnull
    @Override
    protected List<PsiElement> getElements(@Nonnull RsStructLiteralBody context) {
        List<PsiElement> result = new ArrayList<>(super.getElements(context));
        PsiElement dotdot = context.getDotdot();
        if (dotdot != null) {
            result.add(dotdot);
        }
        return result;
    }

    @Nonnull
    @Override
    protected PsiElement getEndElement(@Nonnull RsStructLiteralBody ctx, @Nonnull PsiElement element) {
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
