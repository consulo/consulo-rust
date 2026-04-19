/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.hints;

import com.intellij.codeInsight.hint.DeclarationRangeHandler;
import com.intellij.openapi.util.TextRange;
import org.jetbrains.annotations.NotNull;
import org.rust.lang.core.psi.RsModItem;

public class RsModItemDeclarationRangeHandler implements DeclarationRangeHandler<RsModItem> {
    @NotNull
    @Override
    public TextRange getDeclarationRange(@NotNull RsModItem container) {
        int startOffset = container.getMod().getTextRange().getStartOffset();
        int endOffset = container.getIdentifier().getTextRange().getEndOffset();
        return new TextRange(startOffset, endOffset);
    }
}
