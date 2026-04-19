/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.spelling;

import com.intellij.psi.impl.source.tree.LeafPsiElement;
import com.intellij.spellchecker.inspections.PlainTextSplitter;
import com.intellij.spellchecker.tokenizer.EscapeSequenceTokenizer;
import com.intellij.spellchecker.tokenizer.TokenConsumer;
import org.jetbrains.annotations.NotNull;
import org.rust.lang.utils.RsEscapesUtils;

public class StringLiteralTokenizer extends EscapeSequenceTokenizer<LeafPsiElement> {

    public static final StringLiteralTokenizer INSTANCE = new StringLiteralTokenizer();

    private StringLiteralTokenizer() {
    }

    @Override
    public void tokenize(@NotNull LeafPsiElement element, @NotNull TokenConsumer consumer) {
        String text = element.getText();

        if (!text.contains("\\")) {
            consumer.consumeToken(element, PlainTextSplitter.getInstance());
        } else {
            processTextWithEscapeSequences(element, text, consumer);
        }
    }

    private static void processTextWithEscapeSequences(@NotNull LeafPsiElement element,
                                                       @NotNull String text,
                                                       @NotNull TokenConsumer consumer) {
        RsEscapesUtils.ParseResult result = RsEscapesUtils.parseRustStringCharacters(text);
        if (result.mySuccess) {
            processTextWithOffsets(element, consumer, result.myOutChars, result.myOffsets, 0);
        }
    }
}
