/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.hints;

import consulo.annotation.component.ExtensionImpl;
import consulo.language.editor.hint.DeclarationRangeHandler;
import consulo.document.util.TextRange;
import jakarta.annotation.Nonnull;
import org.rust.lang.core.psi.RsModItem;

@ExtensionImpl
public class RsModItemDeclarationRangeHandler implements DeclarationRangeHandler<RsModItem> {

    @Override
    public Class<RsModItem> getElementClass() {
        return RsModItem.class;
    }

    @Nonnull
    @Override
    public TextRange getDeclarationRange(@Nonnull RsModItem container) {
        int startOffset = container.getMod().getTextRange().getStartOffset();
        int endOffset = container.getIdentifier().getTextRange().getEndOffset();
        return new TextRange(startOffset, endOffset);
    }
}
