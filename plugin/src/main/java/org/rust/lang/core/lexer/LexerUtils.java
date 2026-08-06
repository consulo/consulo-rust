/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.lexer;

import consulo.language.lexer.Lexer;
import consulo.language.ast.IElementType;
import consulo.util.lang.Pair;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public final class LexerUtils {

    private LexerUtils() {
    }

    /**
     * Tokenizes a CharSequence using the given lexer, returning a sequence of (IElementType, String) pairs.
     */
    @Nonnull
    public static Iterable<Pair<IElementType, String>> tokenize(@Nonnull CharSequence text, @Nonnull Lexer lexer) {
        List<Pair<IElementType, String>> result = new ArrayList<>();
        lexer.start(text);
        while (((consulo.language.ast.IElementType) lexer.getTokenType()) != null) {
            result.add(new Pair<>(lexer.getTokenType(), lexer.getTokenText()));
            lexer.advance();
        }
        return result;
    }

    /**
     * Gets the Rust lexer token type for the given string, or null if the string doesn't represent a single token.
     */
    @Nullable
    public static IElementType getRustLexerTokenType(@Nonnull String text) {
        RsLexer lexer = new RsLexer();
        lexer.start(text);
        if (lexer.getTokenEnd() == text.length()) {
            return lexer.getTokenType();
        }
        return null;
    }
}
