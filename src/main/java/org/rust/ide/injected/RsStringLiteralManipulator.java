/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.injected;

import com.intellij.openapi.util.TextRange;
import com.intellij.psi.AbstractElementManipulator;
import org.jetbrains.annotations.NotNull;
import org.rust.lang.core.psi.RsLitExpr;
import org.rust.lang.core.psi.RsLiteralKind;
import org.rust.lang.core.psi.RsLiteralKindUtil;
import org.rust.lang.core.psi.RsPsiFactory;

public class RsStringLiteralManipulator extends AbstractElementManipulator<RsLitExpr> {

    @Override
    public RsLitExpr handleContentChange(@NotNull RsLitExpr element, @NotNull TextRange range, @NotNull String newContent) {
        if (!range.equals(getRangeInElement(element))) {
            return element;
        }

        String oldText = element.getText();
        String newText = oldText.substring(0, range.getStartOffset()) + newContent + oldText.substring(range.getEndOffset());

        RsLitExpr newLitExpr = (RsLitExpr) new RsPsiFactory(element.getProject()).createExpression(newText);
        return (RsLitExpr) element.replace(newLitExpr);
    }

    @NotNull
    @Override
    public TextRange getRangeInElement(@NotNull RsLitExpr element) {
        RsLiteralKind kind = RsLiteralKindUtil.getKind(element);
        if (kind instanceof RsLiteralKind.StringLiteral) {
            TextRange value = ((RsLiteralKind.StringLiteral) kind).getOffsets().getValue();
            if (value != null) return value;
        }
        return super.getRangeInElement(element);
    }
}
