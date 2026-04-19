/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.hints;

import com.intellij.codeInsight.hint.DeclarationRangeHandler;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;
import org.rust.lang.core.psi.RsEnumItem;
import org.rust.lang.core.psi.ext.RsElementUtil;

public class RsEnumItemDeclarationRangeHandler implements DeclarationRangeHandler<RsEnumItem> {
    @NotNull
    @Override
    public TextRange getDeclarationRange(@NotNull RsEnumItem container) {
        int startOffset = container.getEnum().getTextRange().getStartOffset();
        PsiElement endElement = container.getEnumBody() != null
            ? RsElementUtil.getPrevNonCommentSibling(container.getEnumBody())
            : null;
        if (endElement == null) {
            endElement = container;
        }
        int endOffset = endElement.getTextRange().getEndOffset();
        return new TextRange(startOffset, endOffset);
    }
}
