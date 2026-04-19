/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.lexer;

import com.intellij.openapi.util.text.StringUtil;
import com.intellij.psi.StringEscapesTokenTypes;
import com.intellij.psi.tree.IElementType;
import com.intellij.psi.tree.TokenSet;
import com.intellij.util.text.CharArrayUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.rust.lang.core.psi.RsElementTypes;

/**
 * Performs lexical analysis of Rust byte/char/string/byte string literals using Rust character escaping rules.
 */
public class RsEscapesLexer extends LexerBaseEx {

    private static final int BYTE_ESCAPE_LENGTH = "\\x00".length();
    private static final int UNICODE_ESCAPE_MIN_LENGTH = "\\u{0}".length();
    private static final int UNICODE_ESCAPE_MAX_LENGTH = "\\u{000000}".length();

    private final IElementType defaultToken;
    private final boolean unicode;
    private final boolean eol;
    private final boolean extendedByte;

    private RsEscapesLexer(@NotNull IElementType defaultToken, boolean unicode, boolean eol, boolean extendedByte) {
        this.defaultToken = defaultToken;
        this.unicode = unicode;
        this.eol = eol;
        this.extendedByte = extendedByte;
    }

    @NotNull
    public IElementType getDefaultToken() {
        return defaultToken;
    }

    public boolean isUnicode() {
        return unicode;
    }

    public boolean isEol() {
        return eol;
    }

    public boolean isExtendedByte() {
        return extendedByte;
    }

    @Nullable
    @Override
    protected IElementType determineTokenType() {
        // We're at the end of the string token => finish lexing
        if (getTokenStart() >= getTokenEnd()) {
            return null;
        }

        // We're not inside escape sequence
        if (getBufferSequence().charAt(getTokenStart()) != '\\') {
            return defaultToken;
        }

        // \ is at the end of the string token
        if (getTokenStart() + 1 >= getTokenEnd()) {
            return StringEscapesTokenTypes.INVALID_CHARACTER_ESCAPE_TOKEN;
        }

        char nextChar = getBufferSequence().charAt(getTokenStart() + 1);
        switch (nextChar) {
            case 'u':
                if (!unicode) {
                    return StringEscapesTokenTypes.INVALID_CHARACTER_ESCAPE_TOKEN;
                }
                if (isValidUnicodeEscape(getTokenStart(), getTokenEnd())) {
                    return StringEscapesTokenTypes.VALID_STRING_ESCAPE_TOKEN;
                }
                return StringEscapesTokenTypes.INVALID_UNICODE_ESCAPE_TOKEN;
            case 'x':
                return EscapeUtils.esc(isValidByteEscape(getTokenStart(), getTokenEnd(), extendedByte));
            case '\r':
            case '\n':
                return EscapeUtils.esc(eol);
            case 'n':
            case 'r':
            case 't':
            case '0':
            case '\\':
            case '\'':
            case '"':
                return StringEscapesTokenTypes.VALID_STRING_ESCAPE_TOKEN;
            default:
                return StringEscapesTokenTypes.INVALID_CHARACTER_ESCAPE_TOKEN;
        }
    }

    @Override
    protected int locateToken(int start) {
        if (start >= getBufferEnd()) {
            return start;
        }

        if (getBufferSequence().charAt(start) == '\\') {
            int i = start + 1;

            if (i >= getBufferEnd()) {
                return getBufferEnd();
            }

            char c = getBufferSequence().charAt(i);
            switch (c) {
                case 'x':
                    if (getBufferEnd() - (i + 1) >= 1 && StringUtil.isHexDigit(getBufferSequence().charAt(i + 1))) {
                        if (getBufferEnd() - (i + 2) >= 1 && StringUtil.isHexDigit(getBufferSequence().charAt(i + 2))) {
                            return i + 2 + 1;
                        } else {
                            return i + 1 + 1;
                        }
                    }
                    break;
                case 'u':
                    if (getBufferEnd() - (i + 1) >= 1 && getBufferSequence().charAt(i + 1) == '{') {
                        int idx = CharArrayUtil.indexOf(getBufferSequence(), "}", i + 1, getBufferEnd());
                        return idx != -1 ? idx + 1 : getBufferEnd();
                    }
                    break;
                case '\r':
                case '\n': {
                    int j = i;
                    while (j < getBufferEnd() && EscapeUtils.isWhitespaceChar(getBufferSequence().charAt(j))) {
                        j++;
                    }
                    return j;
                }
            }
            return i + 1;
        } else {
            int idx = CharArrayUtil.indexOf(getBufferSequence(), "\\", start + 1, getBufferEnd());
            return idx != -1 ? idx : getBufferEnd();
        }
    }

    private boolean isValidByteEscape(int start, int end, boolean extended) {
        return end - start == BYTE_ESCAPE_LENGTH
            && charSequenceStartsWith(getBufferSequence(), "\\x", start)
            && testCodepointRange(start + 2, end, extended ? 0xff : 0x7f);
    }

    private boolean isValidUnicodeEscape(int start, int end) {
        // FIXME(mkaput): I'm not sure if this max codepoint is correct.
        String sub = getBufferSequence().subSequence(start, end).toString();
        long nonUnderscoreCount = sub.chars().filter(ch -> ch != '_').count();
        return nonUnderscoreCount >= UNICODE_ESCAPE_MIN_LENGTH
            && nonUnderscoreCount <= UNICODE_ESCAPE_MAX_LENGTH
            && charSequenceStartsWith(getBufferSequence(), "\\u{", start)
            && getBufferSequence().charAt(end - 1) == '}'
            && testCodepointRange(start + 3, end - 1, 0x10ffff);
    }

    private boolean testCodepointRange(int start, int end, int max) {
        try {
            String range = getBufferSequence().subSequence(start, end).toString();
            if (range.startsWith("_")) {
                return false;
            }
            String filtered = range.replace("_", "");
            return Integer.parseInt(filtered, 16) <= max;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    private static boolean charSequenceStartsWith(@NotNull CharSequence seq, @NotNull String prefix, int offset) {
        if (offset + prefix.length() > seq.length()) return false;
        for (int i = 0; i < prefix.length(); i++) {
            if (seq.charAt(offset + i) != prefix.charAt(i)) return false;
        }
        return true;
    }

    // Companion object members as static

    /**
     * Create an instance of {@link RsEscapesLexer} suitable for given {@link IElementType}.
     *
     * For the set of supported token types see {@link #ESCAPABLE_LITERALS_TOKEN_SET}.
     *
     * @throws IllegalArgumentException when given token type is unsupported
     */
    @NotNull
    public static RsEscapesLexer of(@NotNull IElementType tokenType) {
        if (tokenType == RsElementTypes.BYTE_LITERAL) {
            return new RsEscapesLexer(RsElementTypes.BYTE_LITERAL, false, false, true);
        } else if (tokenType == RsElementTypes.CHAR_LITERAL) {
            return new RsEscapesLexer(RsElementTypes.CHAR_LITERAL, true, false, false);
        } else if (tokenType == RsElementTypes.BYTE_STRING_LITERAL) {
            return new RsEscapesLexer(RsElementTypes.BYTE_STRING_LITERAL, false, true, true);
        } else if (tokenType == RsElementTypes.CSTRING_LITERAL) {
            return new RsEscapesLexer(RsElementTypes.CSTRING_LITERAL, true, true, true);
        } else if (tokenType == RsElementTypes.STRING_LITERAL) {
            return new RsEscapesLexer(RsElementTypes.STRING_LITERAL, true, true, false);
        } else {
            throw new IllegalArgumentException("unsupported literal type: " + tokenType);
        }
    }

    /**
     * Create an instance of {@link RsEscapesLexer} suitable for situations
     * when there is no need to care about token types.
     *
     * There are no constraints on the value of {@link RsEscapesLexer#defaultToken} in dummy instances.
     */
    @NotNull
    public static RsEscapesLexer dummy(boolean unicode, boolean eol, boolean extendedByte) {
        return new RsEscapesLexer(RsElementTypes.STRING_LITERAL, unicode, eol, extendedByte);
    }

    @NotNull
    public static RsEscapesLexer dummy() {
        return dummy(true, true, true);
    }

    /**
     * Set of possible arguments for {@link #of(IElementType)}
     */
    @NotNull
    public static final TokenSet ESCAPABLE_LITERALS_TOKEN_SET = TokenSet.create(
        RsElementTypes.BYTE_LITERAL,
        RsElementTypes.CHAR_LITERAL,
        RsElementTypes.STRING_LITERAL,
        RsElementTypes.BYTE_STRING_LITERAL,
        RsElementTypes.CSTRING_LITERAL
    );
}
