/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.lexer;

import consulo.language.lexer.LexerBase;
import consulo.language.ast.IElementType;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

/**
 * Small utility class to ease implementing {@link LexerBase}.
 */
public abstract class LexerBaseEx extends LexerBase {
    private int state = 0;
    private int tokenStart = 0;
    private int tokenEnd = 0;
    private CharSequence bufferSequence;
    private int bufferEnd = 0;
    private IElementType tokenType = null;

    /**
     * Determine type of the current token (the one delimited by {@code tokenStart} and {@code tokenEnd}).
     */
    @Nullable
    protected abstract IElementType determineTokenType();

    /**
     * Find next token location (the one starting with {@code tokenEnd} and ending somewhere).
     *
     * @return end offset of the next token
     */
    protected abstract int locateToken(int start);

    @Override
    public void start(@Nonnull CharSequence buffer, int startOffset, int endOffset, int initialState) {
        bufferSequence = buffer;
        bufferEnd = endOffset;
        state = initialState;

        tokenEnd = startOffset;
        advance();
    }

    @Override
    public void advance() {
        tokenStart = tokenEnd;
        tokenEnd = locateToken(tokenStart);
        tokenType = determineTokenType();
    }

    @Nullable
    @Override
    public IElementType getTokenType() {
        return tokenType;
    }

    @Override
    public int getState() {
        return state;
    }

    @Override
    public int getTokenStart() {
        return tokenStart;
    }

    @Override
    public int getTokenEnd() {
        return tokenEnd;
    }

    @Nonnull
    @Override
    public CharSequence getBufferSequence() {
        return bufferSequence;
    }

    @Override
    public int getBufferEnd() {
        return bufferEnd;
    }
}
