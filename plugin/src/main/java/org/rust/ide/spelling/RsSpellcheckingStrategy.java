/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.spelling;

import consulo.language.psi.PsiElement;
import consulo.language.spellcheker.SpellcheckingStrategy;
import consulo.language.spellcheker.tokenizer.Tokenizer;
import jakarta.annotation.Nonnull;
import org.rust.lang.RsLanguage;
import org.rust.lang.core.psi.RsElementTypes;
import org.rust.lang.core.psi.ext.RsNameIdentifierOwner;

public class RsSpellcheckingStrategy extends SpellcheckingStrategy {
    @jakarta.annotation.Nonnull @Override public consulo.language.Language getLanguage() { return org.rust.lang.RsLanguage.INSTANCE; }


    @Override
    public boolean isMyContext(@Nonnull PsiElement element) {
        return RsLanguage.INSTANCE.is(element.getLanguage());
    }

    @Override
    public @Nonnull Tokenizer<?> getTokenizer(PsiElement element) {
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
