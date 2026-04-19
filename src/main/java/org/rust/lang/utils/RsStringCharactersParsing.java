/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.utils;

import org.jetbrains.annotations.NotNull;

/**
 * Stub for Rust string character parsing utilities.
 */
public final class RsStringCharactersParsing {
    private RsStringCharactersParsing() {
    }

    public static class Result {
        private final String value;
        private final int[] offsets;

        public Result(@NotNull String value, int[] offsets) {
            this.value = value;
            this.offsets = offsets;
        }

        @NotNull
        public String getValue() {
            return value;
        }

        @NotNull
        public CharSequence getParsedText() {
            return value;
        }

        public int[] getOffsets() {
            return offsets;
        }

        public int[] getSourceMap() {
            return offsets;
        }

        public int getOffsetByIndex(int index) {
            return offsets != null && index < offsets.length ? offsets[index] : index;
        }
    }

    /**
     * Parses Rust string escape sequences and returns the unescaped text plus a source-offset
     * map sized {@code outChars.length() + 1}. Mirrors {@code parseRustStringCharacters} from
     * as well as numeric escapes (\\xNN, \\u{NNNN}) and line-continuation escapes
     * (backslash + newline).
     */
    @NotNull
    public static Result parseRustStringCharacters(@NotNull String text) {
        int n = text.length();
        StringBuilder out = new StringBuilder(n);
        // sourceOffsets[i] = index in source text that produced out.charAt(i); the trailing entry
        // points one past the final consumed character so that callers can use it as an end marker.
        int[] sourceOffsets = new int[n + 1];

        int src = 0;
        int dst = 0;
        while (src < n) {
            char c = text.charAt(src);
            if (c != '\\') {
                sourceOffsets[dst] = src;
                out.append(c);
                dst++;
                src++;
                continue;
            }
            if (src + 1 >= n) {
                // dangling backslash — emit it verbatim
                sourceOffsets[dst] = src;
                out.append(c);
                dst++;
                src++;
                continue;
            }
            int escStart = src;
            char next = text.charAt(src + 1);
            switch (next) {
                case 'n': out.append('\n'); src += 2; break;
                case 'r': out.append('\r'); src += 2; break;
                case 't': out.append('\t'); src += 2; break;
                case '0': out.append('\0'); src += 2; break;
                case '\\': out.append('\\'); src += 2; break;
                case '\'': out.append('\''); src += 2; break;
                case '"': out.append('"'); src += 2; break;
                case '\n': src += 2; continue; // line continuation — emit nothing
                case '\r':
                    src += 2;
                    if (src < n && text.charAt(src) == '\n') src++;
                    continue;
                case 'x': {
                    int end = Math.min(src + 4, n);
                    if (end - (src + 2) == 2) {
                        try {
                            int code = Integer.parseInt(text.substring(src + 2, end), 16);
                            out.append((char) code);
                            src = end;
                            break;
                        } catch (NumberFormatException ignored) {
                            // fall through, treat as literal backslash
                        }
                    }
                    sourceOffsets[dst] = src;
                    out.append(c);
                    dst++;
                    src++;
                    continue;
                }
                case 'u': {
                    if (src + 2 < n && text.charAt(src + 2) == '{') {
                        int end = text.indexOf('}', src + 3);
                        if (end > src + 3) {
                            String hex = text.substring(src + 3, end).replace("_", "");
                            try {
                                int code = Integer.parseInt(hex, 16);
                                out.appendCodePoint(code);
                                int written = Character.charCount(code);
                                for (int k = 0; k < written; k++) sourceOffsets[dst + k] = escStart;
                                dst += written;
                                src = end + 1;
                                continue;
                            } catch (NumberFormatException ignored) {
                                // fall through
                            }
                        }
                    }
                    sourceOffsets[dst] = src;
                    out.append(c);
                    dst++;
                    src++;
                    continue;
                }
                default:
                    // Unknown escape: emit backslash literally, let outer code see the next char.
                    sourceOffsets[dst] = src;
                    out.append(c);
                    dst++;
                    src++;
                    continue;
            }
            sourceOffsets[dst] = escStart;
            dst++;
        }
        sourceOffsets[dst] = src;

        // Trim sourceOffsets to dst+1 entries.
        int[] trimmed = new int[dst + 1];
        System.arraycopy(sourceOffsets, 0, trimmed, 0, dst + 1);
        return new Result(out.toString(), trimmed);
    }
}
