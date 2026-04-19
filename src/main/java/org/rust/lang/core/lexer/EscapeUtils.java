/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.lexer;

import com.intellij.lexer.Lexer;
import com.intellij.psi.StringEscapesTokenTypes;
import com.intellij.psi.tree.IElementType;
import com.intellij.openapi.util.Pair;
import org.jetbrains.annotations.NotNull;

import java.util.Iterator;
import java.util.function.Function;

public final class EscapeUtils {

    private EscapeUtils() {
    }

    @NotNull
    public static IElementType esc(boolean test) {
        return test ? StringEscapesTokenTypes.VALID_STRING_ESCAPE_TOKEN : StringEscapesTokenTypes.INVALID_CHARACTER_ESCAPE_TOKEN;
    }

    /**
     * Determines if the char is a whitespace character.
     */
    public static boolean isWhitespaceChar(char c) {
        return c == ' ' || c == '\r' || c == '\n' || c == '\t';
    }

    /**
     * Mimics {@link com.intellij.codeInsight.CodeInsightUtilCore#parseStringCharacters}
     * but obeys specific escaping rules provided by {@code decoder}.
     */
    public static boolean parseStringCharacters(
        @NotNull Lexer lexer,
        @NotNull String chars,
        @NotNull StringBuilder outChars,
        @NotNull int[] sourceOffsets,
        @NotNull Function<String, String> decoder
    ) {
        int outOffset = outChars.length();
        int index = 0;
        Iterator<Pair<IElementType, String>> iterator = LexerUtils.tokenize(chars, lexer).iterator();
        while (iterator.hasNext()) {
            Pair<IElementType, String> pair = iterator.next();
            IElementType type = pair.getFirst();
            String text = pair.getSecond();
            // Set offset for the decoded character to the beginning of the escape sequence.
            sourceOffsets[outChars.length() - outOffset] = index;
            sourceOffsets[outChars.length() - outOffset + 1] = index + 1;
            if (type == StringEscapesTokenTypes.VALID_STRING_ESCAPE_TOKEN) {
                outChars.append(decoder.apply(text));
                // And perform a "jump"
                index += text.length();
            } else if (type == StringEscapesTokenTypes.INVALID_CHARACTER_ESCAPE_TOKEN
                || type == StringEscapesTokenTypes.INVALID_UNICODE_ESCAPE_TOKEN) {
                return false;
            } else {
                int first = outChars.length() - outOffset;
                outChars.append(text);
                int last = outChars.length() - outOffset - 1;
                // Set offsets for each character of given chunk
                for (int i = first; i <= last; i++) {
                    sourceOffsets[i] = index;
                    index++;
                }
            }
        }

        sourceOffsets[outChars.length() - outOffset] = index;

        return true;
    }
}
