/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.typing;

import consulo.codeEditor.Editor;
import consulo.codeEditor.EditorEx;
import consulo.codeEditor.HighlighterIterator;
import consulo.language.ast.TokenSet;
import jakarta.annotation.Nonnull;
import org.rust.lang.core.psi.RsElementTypes;
import consulo.language.ast.IElementType;

/**
 * Handlers for angle brace auto-completion in Rust.
 */
public final class RsAngleBraceHandlers {

    private RsAngleBraceHandlers() {
    }

    private static final TokenSet GENERIC_NAMED_ENTITY_KEYWORDS = TokenSet.create(
        RsElementTypes.FN, RsElementTypes.STRUCT, RsElementTypes.ENUM, RsElementTypes.TRAIT, RsElementTypes.TYPE_KW
    );

    private static final TokenSet INVALID_INSIDE_TOKENS = TokenSet.create(
        RsElementTypes.LBRACE, RsElementTypes.RBRACE, RsElementTypes.SEMICOLON
    );

    public static final RsBraceHandlers.BraceHandler ANGLE_BRACE_HANDLER = new RsBraceHandlers.BraceHandler() {
        @Nonnull
        @Override
        public RsBraceHandlers.BraceKind getOpening() {
            return new RsBraceHandlers.BraceKind('<', RsElementTypes.LT);
        }

        @Nonnull
        @Override
        public RsBraceHandlers.BraceKind getClosing() {
            return new RsBraceHandlers.BraceKind('>', RsElementTypes.GT);
        }

        @Override
        public boolean shouldComplete(@Nonnull Editor editor) {
            int offset = editor.getCaretModel().getOffset();
            HighlighterIterator lexer = TypingUtil.createLexer(editor, offset - 1);
            if (lexer == null) return false;

            if (((consulo.language.ast.IElementType) lexer.getTokenType()) == RsElementTypes.COLONCOLON) return true;
            if (((consulo.language.ast.IElementType) lexer.getTokenType()) == RsElementTypes.IMPL) return true;
            if (((consulo.language.ast.IElementType) lexer.getTokenType()) == RsElementTypes.IDENTIFIER) {
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
        public int calculateBalance(@Nonnull Editor editor) {
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
                if (((consulo.language.ast.IElementType) iterator.getTokenType()) == RsElementTypes.LT) balance++;
                else if (((consulo.language.ast.IElementType) iterator.getTokenType()) == RsElementTypes.GT) balance--;
                iterator.advance();
            }
            return balance;
        }

        private boolean isTypeLikeIdentifier(int offset, @Nonnull Editor editor, @Nonnull HighlighterIterator iterator) {
            if (iterator.getEnd() != offset) return false;
            CharSequence chars = editor.getDocument().getCharsSequence();
            if (!Character.isUpperCase(chars.charAt(iterator.getStart()))) return false;
            if (iterator.getEnd() == iterator.getStart() + 1) return true;
            for (int i = iterator.getStart() + 1; i < iterator.getEnd(); i++) {
                if (Character.isLowerCase(chars.charAt(i))) return true;
            }
            return false;
        }
    };

    public static class RsAngleBraceTypedHandler extends RsBraceHandlers.RsBraceTypedHandler {
        public RsAngleBraceTypedHandler() {
            super(ANGLE_BRACE_HANDLER);
        }
    }

    public static class RsAngleBraceBackspaceHandler extends RsBraceHandlers.RsBraceBackspaceHandler {
        public RsAngleBraceBackspaceHandler() {
            super(ANGLE_BRACE_HANDLER);
        }
    }
}
