/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.utils;

import com.intellij.openapi.util.text.StringUtil;
import com.intellij.psi.StringEscapesTokenTypes;
import com.intellij.openapi.util.Pair;
import org.jetbrains.annotations.NotNull;
import org.rust.lang.core.lexer.EscapeUtils;
import org.rust.lang.core.lexer.LexerUtils;
import org.rust.lang.core.lexer.RsEscapesLexer;

/**
 * Unescape string escaped using Rust escaping rules.
 */
public final class RsEscapesUtils {
    private RsEscapesUtils() {
    }

    @NotNull
    public static String unescapeRust(@NotNull String text) {
        return unescapeRust(text, true, true, true);
    }

    @NotNull
    public static String unescapeRust(@NotNull String text, boolean unicode, boolean eol, boolean extendedByte) {
        return unescapeRust(text, RsEscapesLexer.dummy(unicode, eol, extendedByte));
    }

    @NotNull
    public static String unescapeRust(@NotNull String text, @NotNull RsEscapesLexer escapesLexer) {
        StringBuilder sb = new StringBuilder();
        for (Pair<com.intellij.psi.tree.IElementType, String> token : LexerUtils.tokenize(text, escapesLexer)) {
            com.intellij.psi.tree.IElementType type = token.getFirst();
            String tokenText = token.getSecond();
            if (type == StringEscapesTokenTypes.VALID_STRING_ESCAPE_TOKEN) {
                sb.append(decodeEscape(tokenText));
            } else {
                sb.append(tokenText);
            }
        }
        return sb.toString();
    }

    @NotNull
    public static ParseResult parseRustStringCharacters(@NotNull String chars) {
        StringBuilder outChars = new StringBuilder();
        ParseOffsetResult offsetResult = parseRustStringCharacters(chars, outChars);
        return new ParseResult(outChars, offsetResult.myOffsets, offsetResult.mySuccess);
    }

    @NotNull
    public static ParseOffsetResult parseRustStringCharacters(@NotNull String chars, @NotNull StringBuilder outChars) {
        int[] sourceOffsets = new int[chars.length() + 1];
        boolean result = parseRustStringCharactersInner(chars, outChars, sourceOffsets);
        return new ParseOffsetResult(sourceOffsets, result);
    }

    private static boolean parseRustStringCharactersInner(
        @NotNull String chars,
        @NotNull StringBuilder outChars,
        @NotNull int[] sourceOffsets
    ) {
        return EscapeUtils.parseStringCharacters(
            RsEscapesLexer.dummy(true, true, true),
            chars,
            outChars,
            sourceOffsets,
            RsEscapesUtils::decodeEscape
        );
    }

    @NotNull
    private static String decodeEscape(@NotNull String esc) {
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

    @NotNull
    public static String escapeRust(@NotNull CharSequence text) {
        return escapeRust(text, true);
    }

    @NotNull
    public static String escapeRust(@NotNull CharSequence text, boolean escapeNonPrintable) {
        StringBuilder sb = new StringBuilder(text.length());
        escapeRust(text, sb, escapeNonPrintable);
        return sb.toString();
    }

    public static void escapeRust(@NotNull CharSequence text, @NotNull StringBuilder out) {
        escapeRust(text, out, true);
    }

    public static void escapeRust(@NotNull CharSequence text, @NotNull StringBuilder out, boolean escapeNonPrintable) {
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
        @NotNull
        public final StringBuilder myOutChars;
        @NotNull
        public final int[] myOffsets;
        public final boolean mySuccess;

        public ParseResult(@NotNull StringBuilder outChars, @NotNull int[] offsets, boolean success) {
            myOutChars = outChars;
            myOffsets = offsets;
            mySuccess = success;
        }
    }

    public static class ParseOffsetResult {
        @NotNull
        public final int[] myOffsets;
        public final boolean mySuccess;

        public ParseOffsetResult(@NotNull int[] offsets, boolean success) {
            myOffsets = offsets;
            mySuccess = success;
        }
    }
}
