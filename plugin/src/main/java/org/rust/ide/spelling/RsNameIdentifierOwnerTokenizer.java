/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.spelling;

import consulo.document.util.TextRange;
import consulo.language.psi.PsiElement;
import consulo.language.psi.util.PsiTreeUtil;
import consulo.language.spellcheker.tokenizer.splitter.IdentifierTokenSplitter;
import consulo.language.spellcheker.tokenizer.TokenConsumer;
import consulo.language.spellcheker.tokenizer.Tokenizer;
import jakarta.annotation.Nonnull;
import org.rust.lang.core.psi.RsElementTypes;
import org.rust.lang.core.psi.RsRawIdentifiers;
import org.rust.lang.core.psi.ext.RsNameIdentifierOwner;

// Inspired by `PsiIdentifierOwnerTokenizer`
public class RsNameIdentifierOwnerTokenizer extends Tokenizer<RsNameIdentifierOwner> {

    public static final RsNameIdentifierOwnerTokenizer INSTANCE = new RsNameIdentifierOwnerTokenizer();

    private RsNameIdentifierOwnerTokenizer() {
    }

    @Override
    public void tokenize(@Nonnull RsNameIdentifierOwner element, @Nonnull TokenConsumer consumer) {
        PsiElement identifier = element.getNameIdentifier();
        if (identifier == null) return;
        TextRange range = identifier.getTextRange();
        if (range.isEmpty()) return;

        int offset = range.getStartOffset() - element.getTextRange().getStartOffset();
        PsiElement parent;
        if (offset < 0) {
            PsiElement commonParent = PsiTreeUtil.findCommonParent(identifier, element);
            if (commonParent == null) return;
            offset = range.getStartOffset() - commonParent.getTextRange().getStartOffset();
            parent = commonParent;
        } else {
            parent = element;
        }
        String text = identifier.getText();
        String unescapedText;
        int offsetShift;
        if (identifier.getNode().getElementType() == RsElementTypes.QUOTE_IDENTIFIER) {
            unescapedText = text.startsWith("'") ? text.substring(1) : text;
            offsetShift = 1;
        } else if (text.startsWith(RsRawIdentifiers.RS_RAW_PREFIX)) {
            unescapedText = RsRawIdentifiers.unescapeIdentifier(text);
            offsetShift = 2;
        } else {
            unescapedText = text;
            offsetShift = 0;
        }
        offset += offsetShift;
        consumer.consumeToken(parent, unescapedText, true, offset, TextRange.allOf(unescapedText), IdentifierTokenSplitter.getInstance());
    }
}
