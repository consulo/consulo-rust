/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.typing;

import consulo.codeEditor.Editor;
import consulo.codeEditor.EditorEx;
import consulo.codeEditor.HighlighterIterator;
import consulo.language.ast.TokenSet;
import org.rust.lang.core.psi.RsElementTypes;
import org.rust.lang.core.psi.RsTokenSetUtil;

public final class AngleBraceHandler implements BraceHandler {

    public static final AngleBraceHandler INSTANCE = new AngleBraceHandler();

    private static final TokenSet GENERIC_NAMED_ENTITY_KEYWORDS = org.rust.lang.core.psi.RsTokenType.tokenSetOf(
        RsElementTypes.FN, RsElementTypes.STRUCT, RsElementTypes.ENUM, RsElementTypes.TRAIT, RsElementTypes.TYPE_KW
    );

    private static final TokenSet INVALID_INSIDE_TOKENS = org.rust.lang.core.psi.RsTokenType.tokenSetOf(
        RsElementTypes.LBRACE, RsElementTypes.RBRACE, RsElementTypes.SEMICOLON
    );

    private final BraceKind myOpening = new BraceKind('<', RsElementTypes.LT);
    private final BraceKind myClosing = new BraceKind('>', RsElementTypes.GT);

    private AngleBraceHandler() {
    }

    @Override
    public BraceKind getOpening() {
        return myOpening;
    }

    @Override
    public BraceKind getClosing() {
        return myClosing;
    }

    @Override
    public boolean shouldComplete(Editor editor) {
        int offset = editor.getCaretModel().getOffset();
        HighlighterIterator lexer = RsBraceHandlersUtil.createLexer(editor, offset - 1);
        if (lexer == null) return false;

        if (((consulo.language.ast.IElementType) lexer.getTokenType()) == RsElementTypes.COLONCOLON) {
            return true;
        }
        if (((consulo.language.ast.IElementType) lexer.getTokenType()) == RsElementTypes.IMPL) {
            return true;
        }
        if (((consulo.language.ast.IElementType) lexer.getTokenType()) == RsElementTypes.IDENTIFIER) {
            // don't complete angle braces inside identifier
            if (lexer.getEnd() != offset) return false;
            if (lexer.getStart() > 1) {
                lexer.retreat();
                lexer.retreat();
                if (GENERIC_NAMED_ENTITY_KEYWORDS.contains(((consulo.language.ast.IElementType) lexer.getTokenType()))) return true;
                lexer.advance();
                lexer.advance();
            }
            return isTypeLikeIdentifier(offset, editor, lexer);
        }
        return false;
    }

    @Override
    public int calculateBalance(Editor editor) {
        int offset = editor.getCaretModel().getOffset() - 1;
        HighlighterIterator iterator = ((EditorEx) editor).getHighlighter().createIterator(offset);
        while (iterator.getStart() > 0 && !INVALID_INSIDE_TOKENS.contains(((consulo.language.ast.IElementType) iterator.getTokenType()))) {
            iterator.retreat();
        }

        if (INVALID_INSIDE_TOKENS.contains(((consulo.language.ast.IElementType) iterator.getTokenType()))) {
            iterator.advance();
        }

        int balance = 0;
        while (!iterator.atEnd() && balance >= 0 && !INVALID_INSIDE_TOKENS.contains(((consulo.language.ast.IElementType) iterator.getTokenType()))) {
            if (((consulo.language.ast.IElementType) iterator.getTokenType()) == RsElementTypes.LT) {
                balance++;
            } else if (((consulo.language.ast.IElementType) iterator.getTokenType()) == RsElementTypes.GT) {
                balance--;
            }
            iterator.advance();
        }
        return balance;
    }

    private boolean isTypeLikeIdentifier(int offset, Editor editor, HighlighterIterator iterator) {
        if (iterator.getEnd() != offset) return false;
        CharSequence chars = editor.getDocument().getCharsSequence();
        if (!Character.isUpperCase(chars.charAt(iterator.getStart()))) return false;
        if (iterator.getEnd() == iterator.getStart() + 1) return true;
        for (int i = iterator.getStart() + 1; i < iterator.getEnd(); i++) {
            if (Character.isLowerCase(chars.charAt(i))) return true;
        }
        return false;
    }
}
