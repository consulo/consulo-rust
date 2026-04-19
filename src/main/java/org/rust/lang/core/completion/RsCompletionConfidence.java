/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.completion;

import org.rust.lang.core.psi.ext.RsElementUtil;
import com.intellij.codeInsight.completion.CompletionConfidence;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.util.ThreeState;
import org.jetbrains.annotations.NotNull;
import org.rust.lang.core.psi.RsElementTypes;
import org.rust.lang.core.psi.RsLetDecl;
import org.rust.lang.core.psi.RsPatBinding;
import org.rust.lang.core.psi.ext.RsElement;

public class RsCompletionConfidence extends CompletionConfidence {
    @NotNull
    @Override
    public ThreeState shouldSkipAutopopup(@NotNull PsiElement contextElement, @NotNull PsiFile psiFile, int offset) {
        // Don't show completion popup when typing a `let binding` identifier starting with a lowercase letter.
        // If the identifier is uppercase, the user probably wants to type a destructuring pattern
        // (`let Foo { ... }`), so we show the completion popup in this case
        if (contextElement.getNode().getElementType() == RsElementTypes.IDENTIFIER) {
            PsiElement parent = contextElement.getParent();
            if (parent instanceof RsPatBinding) {
                RsPatBinding binding = (RsPatBinding) parent;
                if (RsElementUtil.getTopLevelPattern(binding).getParent() instanceof RsLetDecl) {
                    CharSequence identText = contextElement.getNode().getChars();
                    int offsetInElement = offset - contextElement.getTextOffset();
                    CharSequence textOnTheLeftOfTheCaret = identText.subSequence(0, Math.min(offsetInElement, identText.length()));
                    if (identText.length() > 0 && Character.isLowerCase(identText.charAt(0))
                        && !"mu".startsWith(textOnTheLeftOfTheCaret.toString())) {
                        return ThreeState.YES;
                    }
                }
            }
        }
        return ThreeState.UNSURE;
    }
}
