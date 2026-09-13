/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.completion;

import org.rust.lang.core.psi.ext.impl.RsElementUtil;
import consulo.language.editor.completion.CompletionConfidence;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiFile;
import consulo.util.lang.ThreeState;
import jakarta.annotation.Nonnull;
import org.rust.lang.core.psi.RsElementTypes;
import org.rust.lang.core.psi.RsLetDecl;
import org.rust.lang.core.psi.RsPatBinding;
import consulo.annotation.component.ExtensionImpl;
import consulo.language.Language;
import org.rust.lang.RsLanguage;

@ExtensionImpl
public class RsCompletionConfidence extends CompletionConfidence {
    @jakarta.annotation.Nonnull @Override public consulo.language.Language getLanguage() { return org.rust.lang.RsLanguage.INSTANCE; }

    @Nonnull
    @Override
    public ThreeState shouldSkipAutopopup(@Nonnull PsiElement contextElement, @Nonnull PsiFile psiFile, int offset) {
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
