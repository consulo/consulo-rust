/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.typing;

import com.intellij.codeInsight.editorActions.MultiCharQuoteHandler;
import com.intellij.codeInsight.editorActions.SimpleTokenSetQuoteHandler;
import com.intellij.openapi.editor.highlighter.HighlighterIterator;
import com.intellij.psi.StringEscapesTokenTypes;
import com.intellij.psi.tree.IElementType;
import com.intellij.openapi.util.Pair;
import org.rust.lang.core.psi.RsLiteralKind;
import org.rust.lang.core.psi.RsTokenType;
import org.rust.lang.core.psi.RsElementTypes;

// Remember not to auto-pair `'` in char literals because of lifetimes, which use single `'`: `'a`
public class RsQuoteHandler extends SimpleTokenSetQuoteHandler implements MultiCharQuoteHandler {

    public RsQuoteHandler() {
        super(
            RsElementTypes.BYTE_LITERAL,
            RsElementTypes.STRING_LITERAL,
            RsElementTypes.BYTE_STRING_LITERAL,
            RsElementTypes.CSTRING_LITERAL,
            RsElementTypes.RAW_STRING_LITERAL,
            RsElementTypes.RAW_BYTE_STRING_LITERAL,
            RsElementTypes.RAW_CSTRING_LITERAL
        );
    }

    @Override
    public boolean isOpeningQuote(HighlighterIterator iterator, int offset) {
        IElementType elementType = iterator.getTokenType();
        int start = iterator.getStart();
        // FIXME: Hashes?
        if (elementType == RsElementTypes.RAW_BYTE_STRING_LITERAL
            || elementType == RsElementTypes.RAW_CSTRING_LITERAL) {
            return offset - start <= 2;
        }
        if (elementType == RsElementTypes.BYTE_STRING_LITERAL
            || elementType == RsElementTypes.CSTRING_LITERAL
            || elementType == RsElementTypes.RAW_STRING_LITERAL) {
            return offset - start <= 1;
        }
        if (elementType == RsElementTypes.BYTE_LITERAL) {
            return offset == start + 1;
        }
        return super.isOpeningQuote(iterator, offset);
    }

    @Override
    public boolean isClosingQuote(HighlighterIterator iterator, int offset) {
        // FIXME: Hashes?
        return super.isClosingQuote(iterator, offset);
    }

    @Override
    public boolean isInsideLiteral(HighlighterIterator iterator) {
        if (StringEscapesTokenTypes.STRING_LITERAL_ESCAPES.contains(iterator.getTokenType())) {
            return true;
        }
        return super.isInsideLiteral(iterator);
    }

    @Override
    public boolean isNonClosedLiteral(HighlighterIterator iterator, CharSequence chars) {
        if (iterator.getTokenType() == RsElementTypes.BYTE_LITERAL) {
            return iterator.getEnd() - iterator.getStart() == 2;
        }
        if (RsTokenType.RS_RAW_LITERALS.contains(iterator.getTokenType())) {
            char lastChar = chars.charAt(iterator.getEnd() - 1);
            return lastChar != '#' && lastChar != '"';
        }
        if (super.isNonClosedLiteral(iterator, chars)) return true;

        advanceToTheLastToken(iterator);

        return myLiteralTokenSet.contains(iterator.getTokenType())
            && TypingUtil.getLiteralDumb(iterator).getOffsets().getCloseDelim() == null;
    }

    private void advanceToTheLastToken(HighlighterIterator iterator) {
        while (!iterator.atEnd()) {
            iterator.advance();
        }
        iterator.retreat();
    }

    /**
     * Check whether caret is deep inside string literal,
     * i.e. it's inside contents itself, not decoration.
     */
    public boolean isDeepInsideLiteral(HighlighterIterator iterator, int offset) {
        // First, filter out unwanted token types
        if (!isInsideLiteral(iterator)) return false;

        IElementType tt = iterator.getTokenType();
        int start = iterator.getStart();

        // If we are inside raw literal then we don't have to deal with escapes
        if (tt == RsElementTypes.RAW_STRING_LITERAL
            || tt == RsElementTypes.RAW_BYTE_STRING_LITERAL
            || tt == RsElementTypes.RAW_CSTRING_LITERAL) {
            RsLiteralKind.RsComplexLiteral literal = TypingUtil.getLiteralDumb(iterator);
            if (literal == null) return false;
            com.intellij.openapi.util.TextRange value = literal.getOffsets().getValue();
            return value != null && value.containsOffset(offset - start);
        }

        // We have to deal with escapes here as we are inside (byte) string literal
        if (StringEscapesTokenTypes.STRING_LITERAL_ESCAPES.contains(tt)) return true;

        Pair<IElementType, IElementType> siblings = TypingUtil.getSiblingTokens(iterator);

        if (!StringEscapesTokenTypes.STRING_LITERAL_ESCAPES.contains(siblings.getFirst())) {
            return !isOpeningQuote(iterator, offset);
        }
        if (!StringEscapesTokenTypes.STRING_LITERAL_ESCAPES.contains(siblings.getSecond())) {
            return !isClosingQuote(iterator, offset - 1);
        }

        return true;
    }

    @Override
    public CharSequence getClosingQuote(HighlighterIterator iterator, int offset) {
        RsLiteralKind.RsComplexLiteral literal = TypingUtil.getLiteralDumb(iterator);
        if (literal == null) return null;
        if (!RsTokenType.RS_RAW_LITERALS.contains(literal.getNode().getElementType())) return null;

        com.intellij.openapi.util.TextRange openDelim = literal.getOffsets().getOpenDelim();
        int hashes = openDelim != null ? openDelim.getLength() - 1 : 0;
        StringBuilder sb = new StringBuilder("\"");
        for (int i = 0; i < hashes; i++) {
            sb.append('#');
        }
        return sb.toString();
    }
}
