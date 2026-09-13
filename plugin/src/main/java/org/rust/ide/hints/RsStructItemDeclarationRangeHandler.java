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
import org.rust.lang.core.psi.RsStructItem;
import org.rust.lang.core.psi.ext.impl.RsElementUtil;
import org.rust.lang.core.psi.ext.impl.RsStructItemUtil;

@ExtensionImpl
public class RsStructItemDeclarationRangeHandler implements DeclarationRangeHandler<RsStructItem> {

    @Override
    public Class<RsStructItem> getElementClass() {
        return RsStructItem.class;
    }

    @Nonnull
    @Override
    public TextRange getDeclarationRange(@Nonnull RsStructItem container) {
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
