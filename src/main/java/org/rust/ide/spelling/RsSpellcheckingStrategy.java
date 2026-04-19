/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.spelling;

import com.intellij.psi.PsiElement;
import com.intellij.spellchecker.tokenizer.SpellcheckingStrategy;
import com.intellij.spellchecker.tokenizer.Tokenizer;
import org.jetbrains.annotations.NotNull;
import org.rust.lang.RsLanguage;
import org.rust.lang.core.psi.RsElementTypes;
import org.rust.lang.core.psi.ext.RsNameIdentifierOwner;

public class RsSpellcheckingStrategy extends SpellcheckingStrategy {

    @Override
    public boolean isMyContext(@NotNull PsiElement element) {
        return RsLanguage.INSTANCE.is(element.getLanguage());
    }

    @Override
    public @NotNull Tokenizer<?> getTokenizer(PsiElement element) {
        if (element != null && element.getNode() != null
            && element.getNode().getElementType() == RsElementTypes.STRING_LITERAL) {
            return StringLiteralTokenizer.INSTANCE;
        }
        if (element instanceof RsNameIdentifierOwner) {
            return RsNameIdentifierOwnerTokenizer.INSTANCE;
        }
        return super.getTokenizer(element);
    }
}
