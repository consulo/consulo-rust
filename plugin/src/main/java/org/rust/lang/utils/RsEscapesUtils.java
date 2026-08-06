/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.utils;

import consulo.util.lang.StringUtil;
import consulo.language.ast.StringEscapesTokenTypes;
import consulo.util.lang.Pair;
import jakarta.annotation.Nonnull;
import org.rust.lang.core.lexer.EscapeUtils;
import org.rust.lang.core.lexer.LexerUtils;
import org.rust.lang.core.lexer.RsEscapesLexer;

/**
 * Unescape string escaped using Rust escaping rules.
 */
public final class RsEscapesUtils {
    private RsEscapesUtils() {
    }

    @Nonnull
    public static String unescapeRust(@Nonnull String text) {
        return unescapeRust(text, true, true, true);
    }

    @Nonnull
    public static String unescapeRust(@Nonnull String text, boolean unicode, boolean eol, boolean extendedByte) {
        return unescapeRust(text, RsEscapesLexer.dummy(unicode, eol, extendedByte));
    }

    @Nonnull
    public static String unescapeRust(@Nonnull String text, @Nonnull RsEscapesLexer escapesLexer) {
        StringBuilder sb = new StringBuilder();
        for (Pair<consulo.language.ast.IElementType, String> token : LexerUtils.tokenize(text, escapesLexer)) {
            consulo.language.ast.IElementType type = token.getFirst();
            String tokenText = token.getSecond();
            if (type == StringEscapesTokenTypes.VALID_STRING_ESCAPE_TOKEN) {
                sb.append(decodeEscape(tokenText));
            } else {
                sb.append(tokenText);
            }
        }
        return sb.toString();
    }

    @Nonnull
    public static ParseResult parseRustStringCharacters(@Nonnull String chars) {
        StringBuilder outChars = new StringBuilder();
        ParseOffsetResult offsetResult = parseRustStringCharacters(chars, outChars);
        return new ParseResult(outChars, offsetResult.myOffsets, offsetResult.mySuccess);
    }

    @Nonnull
    public static ParseOffsetResult parseRustStringCharacters(@Nonnull String chars, @Nonnull StringBuilder outChars) {
        int[] sourceOffsets = new int[chars.length() + 1];
        boolean result = parseRustStringCharactersInner(chars, outChars, sourceOffsets);
        return new ParseOffsetResult(sourceOffsets, result);
    }

    private static boolean parseRustStringCharactersInner(
        @Nonnull String chars,
        @Nonnull StringBuilder outChars,
        @Nonnull int[] sourceOffsets
    ) {
        return EscapeUtils.parseStringCharacters(
            RsEscapesLexer.dummy(true, true, true),
            chars,
            outChars,
            sourceOffsets,
            RsEscapesUtils::decodeEscape
        );
    }

    @Nonnull
    private static String decodeEscape(@Nonnull String esc) {
        switch (esc) {
            case "\\n": return "\n";
            case "\\r": return "\r";
            case "\\t": return "\t";
            case "\\0": return "\u0000";
            case "\\\\": return "\\";
            case "\\'": return "'";
            case "\\\"": return "\"";
            default: {
                assert esc.length() >= 2;
                assert esc.charAt(0) == '\\';
                char second = esc.charAt(1);
                if (second == 'x') {
                    return String.valueOf((char) Integer.parseInt(esc.substring(2), 16));
                } else if (second == 'u') {
                    String filtered = esc.substring(3, esc.length() - 1).replace("_", "");
                    return String.valueOf((char) Integer.parseInt(filtered, 16));
                } else if (second == '\r' || second == '\n') {
                    return "";
                } else {
                    throw new IllegalStateException("unreachable");
                }
            }
        }
    }

    @Nonnull
    public static String escapeRust(@Nonnull CharSequence text) {
        return escapeRust(text, true);
    }

    @Nonnull
    public static String escapeRust(@Nonnull CharSequence text, boolean escapeNonPrintable) {
        StringBuilder sb = new StringBuilder(text.length());
        escapeRust(text, sb, escapeNonPrintable);
        return sb.toString();
    }

    public static void escapeRust(@Nonnull CharSequence text, @Nonnull StringBuilder out) {
        escapeRust(text, out, true);
    }

    public static void escapeRust(@Nonnull CharSequence text, @Nonnull StringBuilder out, boolean escapeNonPrintable) {
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c == '\n') {
                out.append("\\n");
            } else if (c == '\r') {
                out.append("\\r");
            } else if (c == '\t') {
                out.append("\\t");
            } else if (c == '\u0000') {
                out.append("\\0");
            } else if (c == '\'') {
                out.append("\\'");
            } else if (c == '"') {
                out.append("\\\"");
            } else if (escapeNonPrintable && !StringUtil.isPrintableUnicode(c)) {
                out.append("\\u{");
                out.append((int) c);
                out.append("}");
            } else {
                out.append(c);
            }
        }
    }

    public static class ParseResult {
        @Nonnull
        public final StringBuilder myOutChars;
        @Nonnull
        public final int[] myOffsets;
        public final boolean mySuccess;

        public ParseResult(@Nonnull StringBuilder outChars, @Nonnull int[] offsets, boolean success) {
            myOutChars = outChars;
            myOffsets = offsets;
            mySuccess = success;
        }
    }

    public static class ParseOffsetResult {
        @Nonnull
        public final int[] myOffsets;
        public final boolean mySuccess;

        public ParseOffsetResult(@Nonnull int[] offsets, boolean success) {
            myOffsets = offsets;
            mySuccess = success;
        }
    }
}
