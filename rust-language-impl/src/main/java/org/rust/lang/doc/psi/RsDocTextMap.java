/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.doc.psi;

import consulo.document.util.TextRange;
import consulo.util.lang.CharArrayUtil;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * A two-way mapping between the raw text of a doc comment token and the Markdown text obtained
 * from it by stripping the comment decoration ({@code ///}, {@code /**}, the leading {@code *}
 * column, the common indent).
 * <p>
 * The raw text is split into {@link Piece}s that are laid out back to back and whose concatenation
 * is byte-for-byte the raw text. That property is what lets the doc comment AST cover the whole
 * token: every character of the token ends up in exactly one leaf.
 */
final class RsDocTextMap {

    enum PieceKind {
        /** Part of the Markdown text. */
        TEXT,
        /** Decoration that is not whitespace, e.g. {@code ///} or a leading {@code *}. */
        GAP,
        /** Whitespace, either decoration or a line break. */
        WHITESPACE
    }

    static final class Piece {
        final CharSequence str;
        final PieceKind kind;

        Piece(@Nonnull CharSequence str, @Nonnull PieceKind kind) {
            this.str = str;
            this.kind = kind;
        }

        @Nonnull
        Piece cut(int startOffset, int endOffset) {
            int from = Math.max(0, startOffset);
            int to = Math.min(endOffset, str.length());
            return new Piece(str.subSequence(from, to), kind);
        }
    }

    private final CharSequence originalText;
    private final String mappedText;
    /** Maps an offset in {@link #mappedText} to an offset in {@link #originalText}. */
    private final int[] offsetMap;
    private final List<Piece> pieces;

    private RsDocTextMap(@Nonnull CharSequence originalText,
                         @Nonnull String mappedText,
                         @Nonnull int[] offsetMap,
                         @Nonnull List<Piece> pieces) {
        this.originalText = originalText;
        this.mappedText = mappedText;
        this.offsetMap = offsetMap;
        this.pieces = pieces;
    }

    @Nonnull
    CharSequence getOriginalText() {
        return originalText;
    }

    @Nonnull
    String getMappedText() {
        return mappedText;
    }

    int mapOffsetToOriginal(int offset) {
        return offsetMap[offset];
    }

    @Nonnull
    TextRange mapTextRangeToOriginal(@Nonnull TextRange range) {
        return new TextRange(mapOffsetToOriginal(range.getStartOffset()), mapOffsetToOriginal(range.getEndOffset()));
    }

    /**
     * Feeds the processor the parts of the pieces that fall into
     * {@code [startOffset, endOffset)} of the original text. Never passes an empty piece.
     */
    void processPiecesInRange(int startOffset, int endOffset, @Nonnull Consumer<Piece> processor) {
        int offset = 0;
        for (Piece piece : pieces) {
            int pieceEndOffset = offset + piece.str.length();
            if (startOffset < pieceEndOffset && endOffset > offset) {
                processor.accept(piece.cut(startOffset - offset, endOffset - offset));
            }
            offset = pieceEndOffset;
        }
    }

    /**
     * Returns the text of {@code range} if that range is covered by a single {@link PieceKind#TEXT}
     * piece, that is, if it contains no decoration at all. Returns {@code null} otherwise.
     */
    @Nullable
    CharSequence mapFully(@Nonnull TextRange range) {
        int offset = 0;
        for (Piece piece : pieces) {
            int pieceEndOffset = offset + piece.str.length();
            if (range.getStartOffset() < pieceEndOffset && range.getEndOffset() > offset) {
                Piece cut = piece.cut(range.getStartOffset() - offset, range.getEndOffset() - offset);
                return cut.kind == PieceKind.TEXT && cut.str.length() == range.getLength() ? cut.str : null;
            }
            offset = pieceEndOffset;
        }
        return null;
    }

    @Nonnull
    static RsDocTextMap create(@Nonnull CharSequence text, @Nonnull RsDocKind kind) {
        List<Piece> pieces = new ArrayList<>();
        StringBuilder mappedText = new StringBuilder();
        int[] map = new int[text.length() + 1];
        int textPosition = 0;

        for (RsDocLine line : kind.removeDecorationToLines(text)) {
            if (line.hasPrefix()) {
                CharSequence prefix = line.getPrefix();
                textPosition += prefix.length();
                addGapWithWhitespace(pieces, prefix);
            }

            if (line.hasContent()) {
                CharSequence content = line.getContent();
                int base = mappedText.length();
                for (int i = 0; i <= content.length(); i++) {
                    map[base + i] = textPosition + i;
                }
                textPosition += content.length();
                mappedText.append(content);
                pieces.add(new Piece(content, PieceKind.TEXT));
            }

            if (line.hasSuffix()) {
                CharSequence suffix = line.getSuffix();
                textPosition += suffix.length();
                addGapWithWhitespace(pieces, suffix);
            }

            if (!line.isLastLine()) {
                if (!line.isRemoved()) {
                    map[mappedText.length()] = textPosition;
                    map[mappedText.length() + 1] = textPosition + 1;
                    mappedText.append('\n');
                }
                textPosition += 1;
                addWhitespace(pieces, "\n");
            }
        }

        return new RsDocTextMap(text, mappedText.toString(), map, pieces);
    }

    /**
     * Splits a piece of decoration into its whitespace and non-whitespace parts so that the
     * non-whitespace part becomes a single {@link PieceKind#GAP}.
     */
    private static void addGapWithWhitespace(@Nonnull List<Piece> pieces, @Nonnull CharSequence str) {
        int gapStart = CharArrayUtil.shiftForward(str, 0, "\n\t ");
        if (gapStart != 0) {
            addWhitespace(pieces, str.subSequence(0, gapStart));
        }
        if (gapStart != str.length()) {
            int gapEnd = CharArrayUtil.shiftBackward(str, gapStart, str.length() - 1, "\n\t ") + 1;
            pieces.add(new Piece(str.subSequence(gapStart, gapEnd), PieceKind.GAP));
            if (gapEnd != str.length()) {
                addWhitespace(pieces, str.subSequence(gapEnd, str.length()));
            }
        }
    }

    private static void addWhitespace(@Nonnull List<Piece> pieces, @Nonnull CharSequence whitespace) {
        int lastIndex = pieces.size() - 1;
        if (lastIndex >= 0 && pieces.get(lastIndex).kind == PieceKind.WHITESPACE) {
            pieces.set(lastIndex, new Piece(pieces.get(lastIndex).str.toString() + whitespace, PieceKind.WHITESPACE));
        }
        else {
            pieces.add(new Piece(whitespace, PieceKind.WHITESPACE));
        }
    }
}
