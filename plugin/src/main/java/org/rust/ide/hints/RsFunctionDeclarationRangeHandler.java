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
import org.rust.lang.core.psi.RsFunction;
import org.rust.lang.core.psi.ext.RsElementUtil;
import org.rust.lang.core.psi.ext.RsFunctionUtil;

@ExtensionImpl
public class RsFunctionDeclarationRangeHandler implements DeclarationRangeHandler<RsFunction> {

    @Override
    public Class<RsFunction> getElementClass() {
        return RsFunction.class;
    }

    @Nonnull
    @Override
    public TextRange getDeclarationRange(@Nonnull RsFunction container) {
        int startOffset = container.getFn().getTextRange().getStartOffset();
        PsiElement endElement = RsFunctionUtil.getBlock(container) != null
            ? RsElementUtil.getPrevNonCommentSibling(RsFunctionUtil.getBlock(container))
            : null;
        if (endElement == null) {
            endElement = container;
        }
        int endOffset = endElement.getTextRange().getEndOffset();
        return new TextRange(startOffset, endOffset);
    }
}
