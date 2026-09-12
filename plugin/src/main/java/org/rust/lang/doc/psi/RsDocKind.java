/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.doc.psi;

import consulo.language.ast.IElementType;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;
import org.rust.lang.core.psi.RsTokenType;

public enum RsDocKind {
    Attr("", "") {
        @Override
        List<RsDocLine> removeDecoration(@Nonnull List<RsDocLine> lines) {
            return removeAttrDecoration(lines);
        }
    },

    InnerBlock("/*!", "*") {
        @Override
        List<RsDocLine> removeDecoration(@Nonnull List<RsDocLine> lines) {
            return removeBlockDecoration(lines);
        }
    },

    OuterBlock("/**", "*") {
        @Override
        List<RsDocLine> removeDecoration(@Nonnull List<RsDocLine> lines) {
            return removeBlockDecoration(lines);
        }
    },

    InnerEol("//!", "//!") {
        @Override
        List<RsDocLine> removeDecoration(@Nonnull List<RsDocLine> lines) {
            return removeEolDecoration(lines);
        }
    },

    OuterEol("///", "///") {
        @Override
        List<RsDocLine> removeDecoration(@Nonnull List<RsDocLine> lines) {
            return removeEolDecoration(lines);
        }
    };

    private final String prefix;
    private final String infix;

    RsDocKind(String prefix, String infix) {
        this.prefix = prefix;
        this.infix = infix;
    }

    public String getPrefix() {
        return prefix;
    }

    public String getInfix() {
        return infix;
    }

    public String getSuffix() {
        return isBlock() ? "*/" : "";
    }

    public boolean isBlock() {
        return this == InnerBlock || this == OuterBlock;
    }

    /**
     * Removes the comment decoration from a list of the token's lines.
     * <p>
     * Expects a non-empty list of lines of a valid doc comment token; performs no validation.
     * <p>
     * The returned lines keep pointing into the original text: the decoration is not cut out of
     * the char sequence but moved into {@link RsDocLine#getPrefix()}/{@link RsDocLine#getSuffix()},
     * so the concatenation of prefix, content and suffix of all lines (interleaved with the line
     * breaks) still reconstructs the original text exactly.
     */
    abstract List<RsDocLine> removeDecoration(@Nonnull List<RsDocLine> lines);

    /**
     * Splits {@code text} into lines and strips the comment decoration off them.
     */
    @Nonnull
    public List<RsDocLine> removeDecorationToLines(@Nonnull CharSequence text) {
        return removeDecoration(RsDocLine.splitLines(text));
    }

    /**
     * Returns the content of the lines that survived the decoration removal.
     */
    @Nonnull
    public Stream<CharSequence> removeDecoration(@Nonnull CharSequence text) {
        return removeDecorationToLines(text).stream()
            .filter(line -> !line.isRemoved())
            .map(RsDocLine::getContent);
    }

    List<RsDocLine> removeEolDecoration(@Nonnull List<RsDocLine> decoratedLines) {
        List<RsDocLine> lines = new ArrayList<>(decoratedLines.size());
        for (RsDocLine line : decoratedLines) {
            lines.add(line.trimStart().removePrefix(infix));
        }
        return removeCommonIndent(lines);
    }

    /**
     * Drops the indent shared by all non-blank lines.
     */
    static List<RsDocLine> removeCommonIndent(@Nonnull List<RsDocLine> decoratedLines) {
        int minIndent = Integer.MAX_VALUE;
        for (RsDocLine line : decoratedLines) {
            if (line.isRemoved() || isBlank(line.getContent())) continue;
            minIndent = Math.min(minIndent, line.countStartWhitespace());
        }

        List<RsDocLine> result = new ArrayList<>(decoratedLines.size());
        for (RsDocLine line : decoratedLines) {
            result.add(line.substring(Math.min(minIndent, line.getContentLength())));
        }
        return result;
    }

    List<RsDocLine> removeBlockDecoration(@Nonnull List<RsDocLine> lines) {
        if (lines.isEmpty()) return List.of();
        List<RsDocLine> result = new ArrayList<>(lines);
        result.set(0, result.get(0).removePrefix(prefix));
        result.set(result.size() - 1, result.get(result.size() - 1).removeSuffix(getSuffix()));
        return removeAttrDecoration(result);
    }

    /**
     * Drops the leading/trailing all-star and blank lines and the common {@code *} column.
     */
    static List<RsDocLine> removeAttrDecoration(@Nonnull List<RsDocLine> lines) {
        if (lines.size() <= 1) return lines;

        List<RsDocLine> trimmed = doVerticalTrim(lines);
        Integer indent = calculateCommonIndentBeforeAsterisk(trimmed);
        if (indent == null) {
            return removeCommonIndent(trimmed);
        }

        List<RsDocLine> unindented = new ArrayList<>(trimmed.size());
        for (RsDocLine line : trimmed) {
            unindented.add(line.substring(Math.min(indent + 1, line.getContentLength())));
        }
        return removeCommonIndent(unindented);
    }

    /**
     * Marks the whitespace-only lines at the start and at the end of the comment as removed.
     */
    private static List<RsDocLine> doVerticalTrim(@Nonnull List<RsDocLine> lines) {
        int start = 0;
        int end = lines.size();

        // A first line consisting of stars only carries no content
        if (consistsOfStars(lines.get(0).getContent())) {
            start++;
        }

        while (start < end && isBlank(lines.get(start).getContent())) {
            start++;
        }

        // Same for the last line, except that its first character is not looked at
        if (end > start) {
            CharSequence lastContent = lines.get(end - 1).getContent();
            if (lastContent.length() == 0
                || consistsOfStars(lastContent.subSequence(1, lastContent.length()))) {
                end--;
            }
        }

        while (end > start && isBlank(lines.get(end - 1).getContent())) {
            end--;
        }

        List<RsDocLine> result = new ArrayList<>(lines);
        for (int i = 0; i < start; i++) {
            result.set(i, result.get(i).markRemoved());
        }
        for (int i = end; i < lines.size(); i++) {
            result.set(i, result.get(i).markRemoved());
        }
        return result;
    }

    /**
     * Returns the column of the leading {@code *} if every kept line has one at the same column,
     * {@code null} otherwise.
     */
    @Nullable
    private static Integer calculateCommonIndentBeforeAsterisk(@Nonnull List<RsDocLine> lines) {
        int indent = Integer.MAX_VALUE;
        boolean first = true;

        for (RsDocLine line : lines) {
            if (line.isRemoved()) continue;
            CharSequence content = line.getContent();
            for (int j = 0; j < content.length(); j++) {
                char c = content.charAt(j);
                if (j > indent || "* \t".indexOf(c) < 0) {
                    return null;
                }
                if (c == '*') {
                    if (first) {
                        indent = j;
                        first = false;
                    }
                    else if (indent != j) {
                        return null;
                    }
                    break;
                }
            }
            if (indent >= line.getContentLength()) {
                return null;
            }
        }
        return indent;
    }

    private static boolean isBlank(@Nonnull CharSequence text) {
        for (int i = 0; i < text.length(); i++) {
            if (!Character.isWhitespace(text.charAt(i))) return false;
        }
        return true;
    }

    private static boolean consistsOfStars(@Nonnull CharSequence text) {
        for (int i = 0; i < text.length(); i++) {
            if (text.charAt(i) != '*') return false;
        }
        return true;
    }

    /**
     * Returns the {@link RsDocKind} of the given doc comment token type.
     *
     * @throws IllegalArgumentException when the token type is not a doc comment one
     */
    @Nonnull
    public static RsDocKind of(@Nonnull IElementType tokenType) {
        if (tokenType == RsTokenType.INNER_BLOCK_DOC_COMMENT) return InnerBlock;
        if (tokenType == RsTokenType.OUTER_BLOCK_DOC_COMMENT) return OuterBlock;
        if (tokenType == RsTokenType.INNER_EOL_DOC_COMMENT) return InnerEol;
        if (tokenType == RsTokenType.OUTER_EOL_DOC_COMMENT) return OuterEol;
        throw new IllegalArgumentException("unsupported token type");
    }
}
