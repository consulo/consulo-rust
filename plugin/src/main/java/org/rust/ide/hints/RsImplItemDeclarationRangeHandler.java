/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.hints;

import consulo.annotation.component.ExtensionImpl;
import consulo.language.editor.hint.DeclarationRangeHandler;
import consulo.document.util.TextRange;
import consulo.language.psi.PsiElement;
import jakarta.annotation.Nonnull;
import org.rust.lang.core.psi.RsImplItem;
import org.rust.lang.core.psi.ext.RsElementUtil;

@ExtensionImpl
public class RsImplItemDeclarationRangeHandler implements DeclarationRangeHandler<RsImplItem> {

    @Override
    public Class<RsImplItem> getElementClass() {
        return RsImplItem.class;
    }

    @Nonnull
    @Override
    public TextRange getDeclarationRange(@Nonnull RsImplItem container) {
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
