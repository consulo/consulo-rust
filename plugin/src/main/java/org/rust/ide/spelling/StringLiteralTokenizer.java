/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.spelling;

import consulo.language.impl.psi.LeafPsiElement;
import consulo.language.spellcheker.tokenizer.splitter.TextTokenSplitter;
import consulo.language.spellcheker.tokenizer.EscapeSequenceTokenizer;
import consulo.language.spellcheker.tokenizer.TokenConsumer;
import jakarta.annotation.Nonnull;
import org.rust.lang.utils.RsEscapesUtils;

public class StringLiteralTokenizer extends EscapeSequenceTokenizer<LeafPsiElement> {

    public static final StringLiteralTokenizer INSTANCE = new StringLiteralTokenizer();

    private StringLiteralTokenizer() {
    }

    @Override
    public void tokenize(@Nonnull LeafPsiElement element, @Nonnull TokenConsumer consumer) {
        String text = element.getText();

        if (!text.contains("\\")) {
            consumer.consumeToken(element, TextTokenSplitter.getInstance());
        } else {
            processTextWithEscapeSequences(element, text, consumer);
        }
    }

    private static void processTextWithEscapeSequences(@Nonnull LeafPsiElement element,
                                                       @Nonnull String text,
                                                       @Nonnull TokenConsumer consumer) {
        RsEscapesUtils.ParseResult result = RsEscapesUtils.parseRustStringCharacters(text);
        if (result.mySuccess) {
            processTextWithOffsets(element, consumer, result.myOutChars, result.myOffsets, 0);
        }
    }
}
