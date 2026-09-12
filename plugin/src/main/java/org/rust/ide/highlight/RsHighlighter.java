/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.highlight;

import consulo.colorScheme.TextAttributesKey;
import consulo.language.editor.highlight.SyntaxHighlighterBase;
import consulo.language.ast.StringEscapesTokenTypes;
import consulo.language.ast.IElementType;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.rust.ide.colors.RsColor;
import org.rust.lang.core.lexer.RsHighlightingLexer;
import org.rust.lang.core.psi.RsElementTypes;
import org.rust.lang.core.psi.RsTokenType;
import org.rust.lang.core.psi.RsTokenSets;
import consulo.language.lexer.Lexer;

public class RsHighlighter extends SyntaxHighlighterBase {

    @Nonnull
    @Override
    public consulo.language.lexer.Lexer getHighlightingLexer() {
        return new RsHighlightingLexer();
    }

    @Nonnull
    @Override
    public TextAttributesKey [] getTokenHighlights(IElementType tokenType) {
        RsColor color = map(tokenType);
        return pack(color != null ? color.getTextAttributesKey() : null);
    }

    @Nullable
    public static RsColor map(@Nonnull IElementType tokenType) {
        if (tokenType == RsElementTypes.QUOTE_IDENTIFIER) return RsColor.LIFETIME;

        if (tokenType == RsElementTypes.CHAR_LITERAL) return RsColor.CHAR;
        if (tokenType == RsElementTypes.BYTE_LITERAL) return RsColor.CHAR;
        if (tokenType == RsElementTypes.STRING_LITERAL) return RsColor.STRING;
        if (tokenType == RsElementTypes.BYTE_STRING_LITERAL) return RsColor.STRING;
        if (tokenType == RsElementTypes.CSTRING_LITERAL) return RsColor.STRING;
        if (tokenType == RsElementTypes.RAW_STRING_LITERAL) return RsColor.STRING;
        if (tokenType == RsElementTypes.RAW_BYTE_STRING_LITERAL) return RsColor.STRING;
        if (tokenType == RsElementTypes.RAW_CSTRING_LITERAL) return RsColor.STRING;
        if (tokenType == RsElementTypes.INTEGER_LITERAL) return RsColor.NUMBER;
        if (tokenType == RsElementTypes.FLOAT_LITERAL) return RsColor.NUMBER;

        if (tokenType == RsTokenType.BLOCK_COMMENT) return RsColor.BLOCK_COMMENT;
        if (tokenType == RsTokenType.EOL_COMMENT) return RsColor.EOL_COMMENT;

        if (RsTokenSets.RS_DOC_COMMENTS.contains(tokenType)) return RsColor.DOC_COMMENT;

        if (tokenType == RsElementTypes.LPAREN || tokenType == RsElementTypes.RPAREN) return RsColor.PARENTHESES;
        if (tokenType == RsElementTypes.LBRACE || tokenType == RsElementTypes.RBRACE) return RsColor.BRACES;
        if (tokenType == RsElementTypes.LBRACK || tokenType == RsElementTypes.RBRACK) return RsColor.BRACKETS;

        if (tokenType == RsElementTypes.SEMICOLON) return RsColor.SEMICOLON;
        if (tokenType == RsElementTypes.DOT) return RsColor.DOT;
        if (tokenType == RsElementTypes.COMMA) return RsColor.COMMA;

        if (tokenType == StringEscapesTokenTypes.VALID_STRING_ESCAPE_TOKEN) return RsColor.VALID_STRING_ESCAPE;
        if (tokenType == StringEscapesTokenTypes.INVALID_CHARACTER_ESCAPE_TOKEN) return RsColor.INVALID_STRING_ESCAPE;
        if (tokenType == StringEscapesTokenTypes.INVALID_UNICODE_ESCAPE_TOKEN) return RsColor.INVALID_STRING_ESCAPE;

        if (tokenType == RsElementTypes.UNSAFE) return RsColor.KEYWORD_UNSAFE;
        if (RsTokenSets.RS_KEYWORDS.contains(tokenType) || tokenType == RsElementTypes.BOOL_LITERAL) return RsColor.KEYWORD;
        if (RsTokenSets.RS_OPERATORS.contains(tokenType)) return RsColor.OPERATORS;

        return null;
    }
}
