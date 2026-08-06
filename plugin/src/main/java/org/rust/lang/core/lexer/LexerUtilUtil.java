/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.lexer;

import consulo.language.ast.IElementType;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

/**
 * Bridge class delegating to {@link LexerUtils}.
 */
public final class LexerUtilUtil {
    private LexerUtilUtil() {
    }

    @Nullable
    public static IElementType getRustLexerTokenType(@Nonnull String text) {
        return LexerUtils.getRustLexerTokenType(text);
    }
}
