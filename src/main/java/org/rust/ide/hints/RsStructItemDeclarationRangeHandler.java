/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.hints;

import com.intellij.codeInsight.hint.DeclarationRangeHandler;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;
import org.rust.lang.core.psi.RsStructItem;
import org.rust.lang.core.psi.ext.RsElementUtil;
import org.rust.lang.core.psi.ext.RsStructItemUtil;

public class RsStructItemDeclarationRangeHandler implements DeclarationRangeHandler<RsStructItem> {
    @NotNull
    @Override
    public TextRange getDeclarationRange(@NotNull RsStructItem container) {
        PsiElement start = container.getStruct();
        if (start == null) {
            start = RsStructItemUtil.getUnion(container);
        }
        if (start == null) {
            start = container;
        }
        int startOffset = start.getTextRange().getStartOffset();
        PsiElement endElement = container.getBlockFields() != null
            ? RsElementUtil.getPrevNonCommentSibling(container.getBlockFields())
            : null;
        if (endElement == null) {
            endElement = container;
        }
        int endOffset = endElement.getTextRange().getEndOffset();
        return new TextRange(startOffset, endOffset);
    }
}
