/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.hints;

import com.intellij.codeInsight.hint.DeclarationRangeHandler;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;
import org.rust.lang.core.psi.RsFunction;
import org.rust.lang.core.psi.ext.RsElementUtil;
import org.rust.lang.core.psi.ext.RsFunctionUtil;

public class RsFunctionDeclarationRangeHandler implements DeclarationRangeHandler<RsFunction> {
    @NotNull
    @Override
    public TextRange getDeclarationRange(@NotNull RsFunction container) {
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
