/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.lexer;

import com.intellij.psi.tree.IElementType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Bridge class delegating to {@link LexerUtils}.
 */
public final class LexerUtilUtil {
    private LexerUtilUtil() {
    }

    @Nullable
    public static IElementType getRustLexerTokenType(@NotNull String text) {
        return LexerUtils.getRustLexerTokenType(text);
    }
}
