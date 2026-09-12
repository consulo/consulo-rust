/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.injected;

import consulo.document.util.TextRange;
import consulo.language.psi.AbstractElementManipulator;
import jakarta.annotation.Nonnull;
import org.rust.lang.core.psi.RsLitExpr;
import org.rust.lang.core.psi.RsLiteralKind;
import org.rust.lang.core.psi.RsLiteralKindUtil;
import org.rust.lang.core.psi.RsPsiFactory;
import consulo.annotation.component.ExtensionImpl;

@ExtensionImpl
public class RsStringLiteralManipulator extends AbstractElementManipulator<RsLitExpr> {

    @Override
    @Nonnull
    public Class<RsLitExpr> getElementClass() {
        return RsLitExpr.class;
    }

    @Override
    public RsLitExpr handleContentChange(@Nonnull RsLitExpr element, @Nonnull TextRange range, @Nonnull String newContent) {
        if (!range.equals(getRangeInElement(element))) {
            return element;
        }

        String oldText = element.getText();
        String newText = oldText.substring(0, range.getStartOffset()) + newContent + oldText.substring(range.getEndOffset());

        RsLitExpr newLitExpr = (RsLitExpr) new RsPsiFactory(element.getProject()).createExpression(newText);
        return (RsLitExpr) element.replace(newLitExpr);
    }

    @Nonnull
    @Override
    public TextRange getRangeInElement(@Nonnull RsLitExpr element) {
        RsLiteralKind kind = RsLiteralKindUtil.getKind(element);
        if (kind instanceof RsLiteralKind.StringLiteral) {
            TextRange value = ((RsLiteralKind.StringLiteral) kind).getOffsets().getValue();
            if (value != null) return value;
        }
        return super.getRangeInElement(element);
    }
}
