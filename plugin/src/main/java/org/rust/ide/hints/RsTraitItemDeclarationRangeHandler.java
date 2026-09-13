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
import org.rust.lang.core.psi.RsTraitItem;
import org.rust.lang.core.psi.ext.impl.RsElementUtil;

@ExtensionImpl
public class RsTraitItemDeclarationRangeHandler implements DeclarationRangeHandler<RsTraitItem> {

    @Override
    public Class<RsTraitItem> getElementClass() {
        return RsTraitItem.class;
    }
    @Nonnull
    @Override
    public TextRange getDeclarationRange(@Nonnull RsTraitItem container) {
        int startOffset = container.getTrait().getTextRange().getStartOffset();
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
