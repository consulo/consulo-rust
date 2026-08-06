/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.hints;

import consulo.language.editor.hint.DeclarationRangeHandler;
import consulo.document.util.TextRange;
import consulo.language.psi.PsiElement;
import jakarta.annotation.Nonnull;
import org.rust.lang.core.psi.RsEnumItem;
import org.rust.lang.core.psi.ext.RsElementUtil;

public class RsEnumItemDeclarationRangeHandler implements DeclarationRangeHandler<RsEnumItem> {

    @Override
    public Class<RsEnumItem> getElementClass() {
        return RsEnumItem.class;
    }

    @Nonnull
    @Override
    public TextRange getDeclarationRange(@Nonnull RsEnumItem container) {
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
