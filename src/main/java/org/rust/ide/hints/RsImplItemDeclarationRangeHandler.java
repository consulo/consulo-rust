/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.hints;

import com.intellij.codeInsight.hint.DeclarationRangeHandler;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;
import org.rust.lang.core.psi.RsImplItem;
import org.rust.lang.core.psi.ext.RsElementUtil;

public class RsImplItemDeclarationRangeHandler implements DeclarationRangeHandler<RsImplItem> {
    @NotNull
    @Override
    public TextRange getDeclarationRange(@NotNull RsImplItem container) {
        int startOffset = container.getImpl().getTextRange().getStartOffset();
        PsiElement endElement = container.getMembers() != null
            ? RsElementUtil.getPrevNonCommentSibling(container.getMembers())
            : null;
        if (endElement == null) {
            endElement = container;
        }
        int endOffset = endElement.getTextRange().getEndOffset();
        return new TextRange(startOffset, endOffset);
    }
}
