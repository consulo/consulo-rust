/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.typing;

import com.intellij.codeInsight.highlighting.PairedBraceMatcherAdapter;
import com.intellij.lang.BracePair;
import com.intellij.lang.PairedBraceMatcher;
import com.intellij.openapi.editor.highlighter.HighlighterIterator;
import com.intellij.openapi.fileTypes.FileType;
import com.intellij.psi.PsiFile;
import com.intellij.psi.TokenType;
import com.intellij.psi.tree.IElementType;
import com.intellij.psi.tree.TokenSet;
import org.rust.lang.RsFileType;
import org.rust.lang.RsLanguage;
import org.rust.lang.core.psi.RsTokenType;
import org.rust.lang.core.psi.RsElementTypes;

import java.util.ArrayDeque;

public class RsBraceMatcher extends PairedBraceMatcherAdapter {

    public static final TokenSet UNPAIRED_TYPE_TOKENS = TokenSet.orSet(
        RsTokenType.RS_COMMENTS,
        org.rust.lang.core.psi.RsTokenType.tokenSetOf(
            TokenType.WHITE_SPACE,
            RsElementTypes.IDENTIFIER, RsElementTypes.UNDERSCORE, RsElementTypes.SELF, RsElementTypes.SUPER,
            RsElementTypes.COMMA, RsElementTypes.SEMICOLON,
            RsElementTypes.QUOTE_IDENTIFIER,
            RsElementTypes.PLUS,
            RsElementTypes.COLON, RsElementTypes.EQ,
            RsElementTypes.COLONCOLON,
            RsElementTypes.INTEGER_LITERAL,
            RsElementTypes.AND, RsElementTypes.MUT, RsElementTypes.CONST, RsElementTypes.MUL,
            RsElementTypes.EXCL
        )
    );

    private static final TokenSet OPEN_BRACES = org.rust.lang.core.psi.RsTokenType.tokenSetOf(
        RsElementTypes.LT, RsElementTypes.LPAREN, RsElementTypes.LBRACE, RsElementTypes.LBRACK
    );

    public RsBraceMatcher() {
        super(new RsBaseBraceMatcher(), RsLanguage.INSTANCE);
    }

    @Override
    public boolean isLBraceToken(HighlighterIterator iterator, CharSequence fileText, FileType fileType) {
        return isBrace(iterator, fileText, fileType, true);
    }

    @Override
    public boolean isRBraceToken(HighlighterIterator iterator, CharSequence fileText, FileType fileType) {
        return isBrace(iterator, fileText, fileType, false);
    }

    private boolean isBrace(HighlighterIterator iterator, CharSequence fileText, FileType fileType, boolean left) {
        if (fileType != RsFileType.INSTANCE) return false;
        BracePair pair = findPair(left, iterator, fileText, fileType);
        if (pair == null) return false;
        IElementType brace = pair.getLeftBraceType();
        // Non angle bracket handled by RsBaseBraceMatcher
        if (!(brace == RsElementTypes.LT || brace == RsElementTypes.GT)) return true;

        int count = 0;
        try {
            ArrayDeque<IElementType> braceStack = new ArrayDeque<>();
            braceStack.addLast(brace);
            boolean prevIsAnd = false;
            while (true) {
                count++;
                if (left) iterator.advance(); else iterator.retreat();
                if (iterator.atEnd()) return false;
                IElementType current = mirrorIfReverse(iterator.getTokenType(), !left);
                if (UNPAIRED_TYPE_TOKENS.contains(current)) {
                    if (prevIsAnd && current == RsElementTypes.AND) return false;
                    prevIsAnd = current == RsElementTypes.AND;
                    continue;
                }
                IElementType co = coBrace(current);
                if (co == null) {
                    if (braceStack.size() == 1) return false;
                    else continue;
                }

                if (OPEN_BRACES.contains(current)) {
                    braceStack.addLast(current);
                } else {
                    IElementType last = braceStack.pollLast();
                    if (last != co) return false;
                    if (braceStack.isEmpty()) return true;
                }
            }
        } finally {
            while (count-- > 0) {
                if (left) iterator.retreat(); else iterator.advance();
            }
        }
    }

    private static IElementType mirrorIfReverse(IElementType b, boolean reverse) {
        if (!reverse) return b;
        IElementType co = coBrace(b);
        return co != null ? co : b;
    }

    private static IElementType coBrace(IElementType b) {
        if (b == RsElementTypes.LT) return RsElementTypes.GT;
        if (b == RsElementTypes.GT) return RsElementTypes.LT;
        if (b == RsElementTypes.LPAREN) return RsElementTypes.RPAREN;
        if (b == RsElementTypes.RPAREN) return RsElementTypes.LPAREN;
        if (b == RsElementTypes.LBRACE) return RsElementTypes.RBRACE;
        if (b == RsElementTypes.RBRACE) return RsElementTypes.LBRACE;
        if (b == RsElementTypes.LBRACK) return RsElementTypes.RBRACK;
        if (b == RsElementTypes.RBRACK) return RsElementTypes.LBRACK;
        return null;
    }

    private static class RsBaseBraceMatcher implements PairedBraceMatcher {

        private static final BracePair[] PAIRS = new BracePair[]{
            new BracePair(RsElementTypes.LBRACE, RsElementTypes.RBRACE, true /* structural */),
            new BracePair(RsElementTypes.LPAREN, RsElementTypes.RPAREN, false),
            new BracePair(RsElementTypes.LBRACK, RsElementTypes.RBRACK, false),
            new BracePair(RsElementTypes.LT, RsElementTypes.GT, false)
        };

        private static final TokenSet INSERT_PAIR_BRACE_BEFORE = TokenSet.orSet(
            RsTokenType.RS_COMMENTS,
            TokenSet.create(
                TokenType.WHITE_SPACE,
                RsElementTypes.SEMICOLON,
                RsElementTypes.COMMA,
                RsElementTypes.RPAREN,
                RsElementTypes.RBRACK,
                RsElementTypes.RBRACE, RsElementTypes.LBRACE
            )
        );

        @Override
        public BracePair[] getPairs() {
            return PAIRS;
        }

        @Override
        public boolean isPairedBracesAllowedBeforeType(IElementType lbraceType, IElementType next) {
            return next == null || INSERT_PAIR_BRACE_BEFORE.contains(next);
        }

        @Override
        public int getCodeConstructStart(PsiFile file, int openingBraceOffset) {
            return openingBraceOffset;
        }
    }
}
